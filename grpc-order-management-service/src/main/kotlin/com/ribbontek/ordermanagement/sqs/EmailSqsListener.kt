package com.ribbontek.ordermanagement.sqs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.ribbontek.ordermanagement.repository.email.EmailRepository
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.BOUNCE
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.COMPLAINT
import com.ribbontek.ordermanagement.repository.email.expectOneByMessageId
import com.ribbontek.ordermanagement.repository.sesnotification.SesNotificationEntity
import com.ribbontek.ordermanagement.repository.sesnotification.SesNotificationRepository
import com.ribbontek.ordermanagement.service.DomainEventPublisher
import com.ribbontek.ordermanagement.service.ReportBounceEmailEvent
import com.ribbontek.ordermanagement.service.ReportComplaintEmailEvent
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.fromJson
import com.ribbontek.shared.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class EmailSqsListener(
    private val sesNotificationRepository: SesNotificationRepository,
    private val emailRepository: EmailRepository
) {
    private val log = logger()

    @ConditionalOnProperty(value = ["sqs.queue.bounce.enabled"], havingValue = "true")
    @SqsListener("\${sqs.queue.bounce.uri}")
    fun handleBounceMessage(message: String) {
        tryRun {
            log.info("Received bounce message: $message")
            val sesNotification = message.fromJson(SesNotification::class.java)
            val sesMessage = sesNotification.message.fromJson(SesMessage::class.java)
            log.info("Bounced Ses Message: $sesMessage")
            sesNotificationRepository.save(sesNotification.toSesNotificationEntity(sesMessage))
            log.info("Saved Ses Notification")
            val email = emailRepository.expectOneByMessageId(sesMessage.mail.messageId)
            emailRepository.save(email.apply { status = BOUNCE })
            log.info("Updated Email Status to BOUNCE")
            DomainEventPublisher.publishEvent(ReportBounceEmailEvent(email.userId))
            log.info("Published ReportComplaintEmailEvent")
        }.onSuccess {
            log.info("Successfully recorded bounced email event")
        }.onFailure {
            log.error("Error processing event", it)
            throw it
        }
    }

    @ConditionalOnProperty(value = ["sqs.queue.complaint.enabled"], havingValue = "true")
    @SqsListener("\${sqs.queue.complaint.uri}")
    fun handleComplaintMessage(message: String) {
        tryRun {
            log.info("Received bounce message: $message")
            val sesNotification = message.fromJson(SesNotification::class.java)
            val sesMessage = sesNotification.message.fromJson(SesMessage::class.java)
            log.info("Complaint Ses Message: $sesMessage")
            sesNotificationRepository.save(sesNotification.toSesNotificationEntity(sesMessage))
            log.info("Saved Ses Notification")
            val email = emailRepository.expectOneByMessageId(sesMessage.mail.messageId)
            emailRepository.save(email.apply { status = COMPLAINT })
            log.info("Updated Email Status to COMPLAINT")
            DomainEventPublisher.publishEvent(ReportComplaintEmailEvent(email.userId))
            log.info("Published ReportComplaintEmailEvent")
        }.onSuccess {
            log.info("Successfully recorded complaint email event")
        }.onFailure {
            log.error("Error processing complaint email event", it)
            throw it
        }
    }

    private fun SesNotification.toSesNotificationEntity(sesMessage: SesMessage): SesNotificationEntity {
        val source = this
        return SesNotificationEntity(
            type = source.type,
            messageId = source.messageId,
            subject = source.subject,
            message = source.message,
            timestamp = source.timestamp,
            sesMessageId = sesMessage.mail.messageId
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesNotification(
    @JsonProperty("Type")
    val type: String,
    @JsonProperty("MessageId")
    val messageId: String,
    @JsonProperty("Subject")
    val subject: String,
    @JsonProperty("Message")
    val message: String,
    @JsonProperty("Timestamp")
    val timestamp: ZonedDateTime
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesMessage(
    val eventType: String,
    val complaint: HashMap<String, *>?,
    val bounce: HashMap<String, *>?,
    val mail: SesMail
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesMail(
    val source: String,
    val messageId: String,
    val destination: List<String>
)
