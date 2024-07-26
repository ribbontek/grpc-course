package com.ribbontek.ordermanagement.sqs

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import com.ribbontek.ordermanagement.config.SesClientConfig
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.repository.email.EmailEntity
import com.ribbontek.ordermanagement.repository.email.EmailRepository
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.SUCCESS
import com.ribbontek.ordermanagement.repository.sesnotification.SesNotificationEntity
import com.ribbontek.ordermanagement.repository.sesnotification.SesNotificationRepository
import com.ribbontek.shared.util.fromJson
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

class EmailSqsListenerTest : AbstractIntegTest() {
    @Autowired
    private lateinit var amazonSimpleEmailService: AmazonSimpleEmailService

    @Autowired
    private lateinit var sesClientConfig: SesClientConfig

    @Autowired
    private lateinit var sesNotificationRepository: SesNotificationRepository

    @Autowired
    private lateinit var emailRepository: EmailRepository

    @Test
    fun `bounce queue - handle successfully`() {
        val recipientEmail = "bounce@simulator.amazonses.com"
        val subject = Content().withData("BOUNCE EMAIL")
        val body = Body().withText(Content().withData("This is a test email."))

        val sendEmailRequest = SendEmailRequest()
            .withSource(sesClientConfig.senderEmail)
            .withReplyToAddresses(sesClientConfig.senderEmail)
            .withDestination(Destination().withToAddresses(recipientEmail))
            .withMessage(Message().withSubject(subject).withBody(body))
            .withConfigurationSetName("GrpcCourseSESConfigurationSet")

        val result = amazonSimpleEmailService.sendEmail(sendEmailRequest)

        emailRepository.save(
            EmailEntity(
                userId = Random.nextLong(),
                fromEmail = sesClientConfig.senderEmail,
                toEmail = recipientEmail,
                status = SUCCESS,
                templateName = "TEST",
                messageId = result.messageId
            )
        )

        var sesNotificationEntity: SesNotificationEntity? = null
        Awaitility.await().pollInSameThread().atMost(10, SECONDS).pollInterval(1, SECONDS).until {
            sesNotificationEntity = sesNotificationRepository.findBySesMessageId(result.messageId)
            sesNotificationEntity != null
        }
        assertNotNull(sesNotificationEntity)
        assertNotNull(sesNotificationEntity?.id)
        assertEquals(sesNotificationEntity?.type, "Notification")
        assertNotNull(sesNotificationEntity?.messageId)
        assertNotNull(sesNotificationEntity?.subject)
        assertNotNull(sesNotificationEntity?.message)
        assertEquals(sesNotificationEntity?.message?.fromJson(SesMessage::class.java)?.eventType, "Bounce")
        assertNotNull(sesNotificationEntity?.sesMessageId)
        assertNotNull(sesNotificationEntity?.timestamp)

        assertEquals(emailRepository.findByMessageId(result.messageId)?.status, EmailStatusEnum.BOUNCE)
    }

    @Test
    fun `complaint queue - handle successfully`() {
        val recipientEmail = "complaint@simulator.amazonses.com"
        val subject = Content().withData("COMPLAINT EMAIL")
        val body = Body().withText(Content().withData("This is a test email."))

        val sendEmailRequest = SendEmailRequest()
            .withSource(sesClientConfig.senderEmail)
            .withReplyToAddresses(sesClientConfig.senderEmail)
            .withDestination(Destination().withToAddresses(recipientEmail))
            .withMessage(Message().withSubject(subject).withBody(body))
            .withConfigurationSetName("GrpcCourseSESConfigurationSet")

        val result = amazonSimpleEmailService.sendEmail(sendEmailRequest)

        emailRepository.save(
            EmailEntity(
                userId = Random.nextLong(),
                fromEmail = sesClientConfig.senderEmail,
                toEmail = recipientEmail,
                status = SUCCESS,
                templateName = "TEST",
                messageId = result.messageId
            )
        )

        var sesNotificationEntity: SesNotificationEntity? = null
        Awaitility.await().pollInSameThread().atMost(10, SECONDS).pollInterval(1, SECONDS).until {
            sesNotificationEntity = sesNotificationRepository.findBySesMessageId(result.messageId)
            sesNotificationEntity != null
        }
        assertNotNull(sesNotificationEntity)
        assertNotNull(sesNotificationEntity?.id)
        assertEquals(sesNotificationEntity?.type, "Notification")
        assertNotNull(sesNotificationEntity?.messageId)
        assertNotNull(sesNotificationEntity?.subject)
        assertNotNull(sesNotificationEntity?.message)
        assertEquals(sesNotificationEntity?.message?.fromJson(SesMessage::class.java)?.eventType, "Complaint")
        assertNotNull(sesNotificationEntity?.sesMessageId)
        assertNotNull(sesNotificationEntity?.timestamp)

        assertEquals(emailRepository.findByMessageId(result.messageId)?.status, EmailStatusEnum.COMPLAINT)
    }
}
