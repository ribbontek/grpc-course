package com.ribbontek.ordermanagement.generator

import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import net.sourceforge.plantuml.SourceStringReader
import net.sourceforge.plantuml.security.SFile
import org.junit.jupiter.api.Assertions.assertTrue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Paths
import javax.sql.DataSource

interface ErdGenerator {
    fun generate()
}

@Component
class ErdGeneratorImpl(
    private val dataSource: DataSource,
    private val entityManager: EntityManager
) : ErdGenerator {
    companion object {
        const val ERD_FILE_NAME = "build/diagrams/erd.png"
    }

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val excludedEntityTables =
        listOf(
            EntityTable("public", "databasechangelog"),
            EntityTable("public", "databasechangeloglock")
        )
    private val excludedViewTables =
        listOf(
            ViewTable("public", "vw_view_table_usage")
        )

    override fun generate() {
        val entityClasses = scanEntityManagerForEntityClasses()
        val entityTables = retrieveEntityTables()

        entityClasses.validateExists(entityTables)

        val pumlText = entityTables.generatePuml()
        try {
            val reader = SourceStringReader(pumlText)
            val file = SFile(Paths.get(ERD_FILE_NAME).toUri())
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            reader.outputImage(file)
            logger.info("ERD Generation Successful")
        } catch (ex: IOException) {
            logger.error("ERD Generation Failure. Caught exception ${ex.message}", ex)
        }
    }

    private fun scanEntityManagerForEntityClasses(): List<EntityClass> =
        entityManager.metamodel.entities.map { entity ->
            entity.javaType.getAnnotation(Table::class.java).let {
                EntityClass(
                    tableName = it.name,
                    schema =
                    it.schema.takeIf { it.isNotBlank() }
                        ?: entityManager.entityManagerFactory.properties["org.hibernate.envers.default_schema"] as? String
                        ?: "public"
                )
            }
        }

    private fun List<DatabaseTable>.generatePuml(): String {
        val pumlBuilder = StringBuilder()
        pumlBuilder.append("@startuml\n")
        pumlBuilder.append("skinparam roundcorner 20\n")
        pumlBuilder.append("skinparam linetype ortho\n")

        forEach { entityTable ->
            when (entityTable) {
                is EntityTable -> entityTable.generateEntity(pumlBuilder)
                is ViewTable -> entityTable.generateEntity(pumlBuilder)
            }
        }

        forEach { table ->
            when (table) {
                is EntityTable -> table.generateRelationships(this@generatePuml, pumlBuilder)
                is ViewTable -> table.generateRelationships(pumlBuilder)
            }
        }

        pumlBuilder.append("@enduml")
        return pumlBuilder.toString()
    }

    private fun ViewTable.generateEntity(pumlBuilder: StringBuilder) {
        pumlBuilder.append("entity \"").append(schema).append(".").append(tableName).append("\" {\n").append("}\n")
    }

    private fun ViewTable.generateRelationships(pumlBuilder: StringBuilder) {
        getAccessTables().forEach { table ->
            pumlBuilder.append(table.schema).append(".").append(table.tableName).append(" .. ")
                .append(schema).append(".").append(tableName).append("\n")
        }
    }

    private fun EntityTable.generateEntity(pumlBuilder: StringBuilder) {
        pumlBuilder.append("entity \"").append(schema).append(".").append(tableName).append("\" {\n")

        getPrimaryKeys().forEach { column ->
            getColumns().find { it.name == column }?.let {
                pumlBuilder.append("  ").append(it.name).append(" : ").append(it.type).append(" (PK)").append("\n")
            }
        }

        if (getPrimaryKeys().isNotEmpty()) pumlBuilder.append("  --\n")

        getColumns()
            .filter { it.name !in getPrimaryKeys() }
            .forEach { column ->
                val isForeignKey = column.name in getForeignKeys().map { it.columnName }
                pumlBuilder.append("  ").append(column.name).append(" : ").append(column.type)
                    .append(if (isForeignKey) " (FK)" else "").append("\n")
            }
        pumlBuilder.append("}\n")
    }

    private fun EntityTable.generateRelationships(
        tables: List<DatabaseTable>,
        pumlBuilder: StringBuilder
    ) {
        getForeignKeys().forEach { foreignKey ->
            val tableColumn = getColumns().find { it.name == foreignKey.columnName }
            tables.filterIsInstance<EntityTable>()
                .find { it.tableName == foreignKey.referencedTableName }
                ?.getColumns()?.find { it.name == foreignKey.referencedColumnName }
                ?.let { referencedColumn ->
                    pumlBuilder
                        .append(foreignKey.referencedSchema).append(".").append(foreignKey.referencedTableName)
                        .append(if (referencedColumn.nullable) " }o" else " }|").append("--")
                        .append(if (tableColumn?.nullable == true) "o| " else "|| ")
                        .append(schema).append(".").append(tableName)
                        .append("\n")
                }
        }
    }

    private fun retrieveEntityTables(): List<DatabaseTable> {
        val databaseTables = mutableListOf<DatabaseTable>()
        val metaData = dataSource.connection.metaData

        metaData.getTables(null, null, null, arrayOf("VIEW")).use { views ->
            while (views.next()) {
                val viewSchema = views.getString("TABLE_SCHEM")
                val viewName = views.getString("TABLE_NAME")
                val viewTable = ViewTable(schema = viewSchema, tableName = viewName)

                if (viewTable in excludedViewTables) continue

                val query =
                    "SELECT DISTINCT table_name, table_schema " +
                        "FROM public.vw_view_table_usage " +
                        "WHERE view_schema = ? AND view_name = ?"
                dataSource.connection.prepareStatement(query).use { preparedStatement ->
                    preparedStatement.setString(1, viewSchema)
                    preparedStatement.setString(2, viewName)
                    preparedStatement.executeQuery().use { tables ->
                        while (tables.next()) {
                            viewTable.addTable(
                                EntityTable(
                                    schema = tables.getString("table_schema"),
                                    tableName = tables.getString("table_name")
                                )
                            )
                        }
                    }
                }
                databaseTables.add(viewTable)
            }
        }

        metaData.getTables(null, null, null, arrayOf("TABLE")).use { tables ->
            while (tables.next()) {
                val tableSchema = tables.getString("TABLE_SCHEM")
                val tableName = tables.getString("TABLE_NAME")
                val entityTable = EntityTable(tableSchema, tableName)

                if (entityTable in excludedEntityTables) continue

                metaData.getColumns(null, tableSchema, tableName, null).use { columns ->
                    while (columns.next()) {
                        entityTable.addColumn(
                            Column(
                                name = columns.getString("COLUMN_NAME"),
                                type = columns.getString("TYPE_NAME"),
                                nullable = columns.getBoolean("NULLABLE")
                            )
                        )
                    }
                }

                metaData.getPrimaryKeys(null, null, tableName).use { primaryKeys ->
                    while (primaryKeys.next()) {
                        entityTable.addPrimaryKey(primaryKeys.getString("COLUMN_NAME"))
                    }
                }

                metaData.getImportedKeys(null, null, tableName).use { importedKeys ->
                    while (importedKeys.next()) {
                        entityTable.addForeignKey(
                            ForeignKey(
                                columnName = importedKeys.getString("FKCOLUMN_NAME"),
                                referencedTableName = importedKeys.getString("PKTABLE_NAME"),
                                referencedColumnName = importedKeys.getString("PKCOLUMN_NAME"),
                                referencedSchema = importedKeys.getString("PKTABLE_SCHEM")
                            )
                        )
                    }
                }
                databaseTables.add(entityTable)
            }
        }
        return databaseTables.toList()
    }

    /**
     * Simple validation method to verify that the entity class exists in the extracted database tables/views
     */
    private fun List<EntityClass>.validateExists(tables: List<DatabaseTable>) {
        val searchTables = tables.toMutableList()
        forEach { entityClass ->
            val result = searchTables.indexOfFirst { it.tableName == entityClass.tableName && it.schema == entityClass.schema }
            assertTrue(result > -1) { "Expected $entityClass to exist in extracted database tables" }
            searchTables.removeAt(result)
        }
    }

    private data class EntityClass(
        override val tableName: String,
        override val schema: String
    ) : DatabaseTable()

    abstract class DatabaseTable {
        abstract val schema: String
        abstract val tableName: String
    }

    private data class ViewTable(
        override val schema: String,
        override val tableName: String
    ) : DatabaseTable() {
        private val accessedTables = mutableSetOf<EntityTable>()

        fun addTable(tableName: EntityTable) {
            accessedTables.add(tableName)
        }

        fun getAccessTables(): List<EntityTable> = accessedTables.toList()
    }

    private data class EntityTable(
        override val schema: String,
        override val tableName: String
    ) : DatabaseTable() {
        private val columns = mutableListOf<Column>()
        private val foreignKeys = mutableListOf<ForeignKey>()
        private val primaryKeys = mutableListOf<String>()

        fun addColumn(column: Column) {
            columns.add(column)
        }

        fun addForeignKey(foreignKey: ForeignKey) {
            foreignKeys.add(foreignKey)
        }

        fun addPrimaryKey(columnName: String) {
            primaryKeys.add(columnName)
        }

        fun getColumns(): List<Column> = columns.toList()

        fun getForeignKeys(): List<ForeignKey> = foreignKeys.toList()

        fun getPrimaryKeys(): List<String> = primaryKeys.toList()
    }

    private data class Column(
        val name: String,
        val type: String,
        val nullable: Boolean
    )

    private data class ForeignKey(
        val columnName: String,
        val referencedSchema: String,
        val referencedTableName: String,
        val referencedColumnName: String
    )
}
