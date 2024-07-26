package com.ribbontek.ordermanagement.repository.audit

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.hibernate.generator.EventType
import java.time.ZonedDateTime

enum class AuditEventType {
    CREATE,
    UPDATE,
    DELETE
}

@Entity
@Table(name = "audit")
@AttributeOverride(name = "id", column = Column(name = "audit_id"))
class AuditEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, updatable = false, nullable = false)
    val id: Long? = null,
    @Column(nullable = false)
    val eventId: String,
    @Column(nullable = false, columnDefinition = "role_type_enum")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    val type: AuditEventType,
    @Column
    val event: String? = null,
    @Column(nullable = false)
    val clazz: String,
    @Column(nullable = false)
    val eventAtUtc: ZonedDateTime
)
