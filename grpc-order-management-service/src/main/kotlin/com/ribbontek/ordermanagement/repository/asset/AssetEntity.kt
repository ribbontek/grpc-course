package com.ribbontek.ordermanagement.repository.asset

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntityDelete
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcType
import org.hibernate.annotations.SQLDelete
import org.hibernate.dialect.PostgreSQLEnumJdbcType

enum class AssetTypeEnum {
    GLOBAL,
    PRODUCT
}

@Entity
@Table(name = "vw_asset")
@AttributeOverride(name = "id", column = Column(name = "asset_id"))
@SQLDelete(sql = "update vw_asset set deleted = true where asset_id = ?")
class AssetEntity(
    @Column(nullable = false, length = 255)
    val name: String,
    @Column(nullable = false)
    var multipart: Boolean = false,
    @Column(nullable = false)
    var partNumber: Int = 1,
    @Column(nullable = false)
    var totalNumber: Int = 1,
    @Column(nullable = false, columnDefinition = "asset_type_enum")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var assetType: AssetTypeEnum,
    @Column(nullable = false, length = 255)
    var assetReference: String
) : AbstractEntityDelete()
