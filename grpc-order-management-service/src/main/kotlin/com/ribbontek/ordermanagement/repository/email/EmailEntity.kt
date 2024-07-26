package com.ribbontek.ordermanagement.repository.email

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType

enum class EmailStatusEnum {
    SUCCESS,
    FAILURE,
    IN_PROGRESS,
    BOUNCE,
    COMPLAINT
}

@Entity
@Table(name = "email")
@AttributeOverride(name = "id", column = Column(name = "email_id"))
class EmailEntity(
    @Column(nullable = false, length = 255)
    val templateName: String,
    @Column(nullable = false)
    val userId: Long,
    @Column(nullable = false, length = 255)
    val fromEmail: String,
    @Column(nullable = true, length = 255)
    val toEmail: String? = null,
    @Column(nullable = true, length = 255)
    var messageId: String? = null,
    @Column(nullable = false, columnDefinition = "email_status_enum")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var status: EmailStatusEnum,
    @PrimaryKeyJoinColumn(name = "email_error_id")
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var emailError: EmailErrorEntity? = null
) : AbstractEntity()
