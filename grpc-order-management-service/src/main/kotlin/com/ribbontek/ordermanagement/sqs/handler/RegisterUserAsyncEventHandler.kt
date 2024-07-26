package com.ribbontek.ordermanagement.sqs.handler

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import com.amazonaws.services.simpleemail.model.SendEmailResult
import com.ribbontek.grpccourse.event.AsyncDomainEvent
import com.ribbontek.grpccourse.event.EventType
import com.ribbontek.grpccourse.event.EventType.REGISTER_USER
import com.ribbontek.ordermanagement.config.EmailTemplateName.WELCOME_EMAIL
import com.ribbontek.ordermanagement.config.SesClientConfig
import com.ribbontek.ordermanagement.repository.email.EmailEntity
import com.ribbontek.ordermanagement.repository.email.EmailErrorEntity
import com.ribbontek.ordermanagement.repository.email.EmailRepository
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.FAILURE
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.IN_PROGRESS
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.SUCCESS
import com.ribbontek.ordermanagement.util.replaceTokens
import com.ribbontek.shared.result.TryResult
import com.ribbontek.shared.result.TryResultFailure
import com.ribbontek.shared.result.TryResultSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.fromJson
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class RegisterUserAsyncEventHandler(
    private val sesClientConfig: SesClientConfig,
    private val amazonSimpleEmailService: AmazonSimpleEmailService,
    private val emailRepository: EmailRepository
) : AsyncEventHandler() {
    override val type: EventType = REGISTER_USER

    @Suppress("unchecked_cast")
    override fun handleEventImpl(event: AsyncDomainEvent) {
        if (!event.hasData()) throw IllegalStateException("Not data provided for event: $REGISTER_USER")
        val eventData = event.data.fromJson(Map::class.java) as Map<String, *>
        val emailData = eventData.toEmailData()
        val emailEntity = emailRepository.saveAndFlush(emailData.toEmailEntity())
        when (val result = sendEmailForRegisterUserEvent(emailData)) {
            is TryResultSuccess -> {
                emailRepository.saveAndFlush(
                    emailEntity.apply {
                        messageId = result.value.sendEmailResult.messageId
                        status = SUCCESS
                    }
                )
                log.info("RegisterUserEventHandler -> completed successfully")
            }
            is TryResultFailure -> {
                log.error("RegisterUserEventHandler -> encountered exception", result.exception)
                emailRepository.saveAndFlush(
                    emailEntity.apply {
                        status = FAILURE
                        emailError =
                            EmailErrorEntity(
                                errorMessage = StringUtils.substring(result.exception.message ?: result.exception.localizedMessage, 0, 1000)
                            )
                    }
                )
                log.info("RegisterUserEventHandler -> completed with error message for user id: ${emailData.userId}")
            }
        }
    }

    private fun EmailData.toEmailEntity(): EmailEntity {
        return EmailEntity(
            userId = userId,
            fromEmail = sesClientConfig.senderEmail,
            toEmail = email,
            status = IN_PROGRESS,
            templateName = WELCOME_EMAIL.name
        )
    }

    private data class EmailData(
        val userId: Long,
        val email: String,
        val unsubscribeCode: String,
        val firstName: String
    )

    private fun Map<String, *>.toEmailData(): EmailData {
        return EmailData(
            userId = this["userId"]!!.toString().toLong(),
            email = this["userEmail"]!!.toString(),
            unsubscribeCode = this["userUnsubscribeCode"]!! as String,
            firstName = this["userFirstName"]!! as String
        )
    }

    private fun sendEmailForRegisterUserEvent(eventData: EmailData): TryResult<Throwable, RegisterUserResult> {
        return tryRun {
            val welcomeEmailTemplate = sesClientConfig.emailTemplates[WELCOME_EMAIL]
                ?: throw IllegalStateException("Could not find email template WELCOME_EMAIL")
            val globalEmailVars = mapOf(
                "headerImageUrl" to sesClientConfig.headerImageUrl,
                "unsubscribeUrl" to "${sesClientConfig.unsubscribeUrl}?c=${eventData.unsubscribeCode}&e=${eventData.email}"
            )
            val userVars = mapOf("firstName" to eventData.firstName)
            val subject = Content().withData(welcomeEmailTemplate.title.replaceTokens(userVars))
            val body = Body().withHtml(
                Content().withData(
                    welcomeEmailTemplate.contentHtml.getContentAsString(Charset.defaultCharset())
                        .replaceTokens(globalEmailVars + userVars)
                )
            )

            val emailRequest = SendEmailRequest()
                .withSource(sesClientConfig.senderEmail)
                .withReplyToAddresses(sesClientConfig.senderEmail)
                .withDestination(Destination().withToAddresses(eventData.email))
                .withMessage(Message().withSubject(subject).withBody(body))
                .withConfigurationSetName("GrpcCourseSESConfigurationSet")

            val sendEmailResult = amazonSimpleEmailService.sendEmail(emailRequest)
            RegisterUserResult(sendEmailResult = sendEmailResult)
        }
    }

    private data class RegisterUserResult(
        val sendEmailResult: SendEmailResult
    )
}
