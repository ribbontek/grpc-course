package com.ribbontek.ordermanagement.repository.sesnotification

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import java.time.ZonedDateTime

@Entity
@Table(name = "ses_notification")
@AttributeOverride(name = "id", column = Column(name = "ses_notification_id"))
class SesNotificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, updatable = false, nullable = false)
    val id: Long? = null,
    @Column(nullable = false)
    val type: String,
    @Column(nullable = false)
    val messageId: String,
    @Column(nullable = false)
    val subject: String,
    @Column(nullable = false)
    val message: String,
    @Column(nullable = false)
    val sesMessageId: String,
    @Column(nullable = false)
    val timestamp: ZonedDateTime
)
