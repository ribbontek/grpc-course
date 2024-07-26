package com.ribbontek.ordermanagement.sqs

import com.ribbontek.grpccourse.event.EventType.REGISTER_USER
import com.ribbontek.grpccourse.event.EventType.REPORT_BOUNCED_EMAIL
import com.ribbontek.grpccourse.event.EventType.REPORT_COMPLAINT_EMAIL
import com.ribbontek.grpccourse.event.asyncDomainEvent
import com.ribbontek.ordermanagement.config.EmailTemplateName.WELCOME_EMAIL
import com.ribbontek.ordermanagement.config.SesClientConfig
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.entity.UserEntityFactory
import com.ribbontek.ordermanagement.repository.email.EmailEntity
import com.ribbontek.ordermanagement.repository.email.EmailRepository
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum
import com.ribbontek.ordermanagement.repository.email.EmailStatusEnum.IN_PROGRESS
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.util.toBase64String
import com.ribbontek.shared.util.toJson
import org.apache.commons.lang3.RandomStringUtils
import org.awaitility.Awaitility.await
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

class EventAsyncQueueListenerTest : AbstractIntegTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var sesClientConfig: SesClientConfig

    @Autowired
    private lateinit var emailRepository: EmailRepository

    @Autowired
    private lateinit var eventAsyncQueueListener: EventAsyncQueueListener

    @Autowired
    private lateinit var asyncQueueMessagingTemplate: QueueMessagingTemplate

    @Test
    fun `handle async event - REPORT_BOUNCED_EMAIL - success`() {
        val user = userRepository.save(UserEntityFactory.create())

        assertFalse(user.emailComplaint)
        assertFalse(user.emailBounced)
        val eventData = mapOf("userId" to user.id)
        val asyncDomainEvent = asyncDomainEvent {
            this.type = REPORT_BOUNCED_EMAIL
            this.data = eventData.toJson()
        }

        asyncQueueMessagingTemplate.convertAndSend(asyncDomainEvent.toBase64String())
        var userEntity: UserEntity? = null
        await().pollInSameThread().atMost(10, SECONDS).pollInterval(1, SECONDS).until {
            userEntity = userRepository.findByIdOrNull(user.id)
            userEntity != null && userEntity?.emailBounced == true
        }

        assertNotNull(userEntity)
        assertFalse(userEntity!!.emailComplaint)
        assertTrue(userEntity!!.emailBounced)
    }

    @Test
    fun `handle async event - REPORT_COMPLAINT_EMAIL - success`() {
        val user = userRepository.save(UserEntityFactory.create())

        assertFalse(user.emailComplaint)
        assertFalse(user.emailBounced)
        val eventData = mapOf("userId" to user.id)
        val asyncDomainEvent = asyncDomainEvent {
            this.type = REPORT_COMPLAINT_EMAIL
            this.data = eventData.toJson()
        }

        asyncQueueMessagingTemplate.convertAndSend(asyncDomainEvent.toBase64String())
        var userEntity: UserEntity? = null
        await().pollInSameThread().atMost(10, SECONDS).pollInterval(1, SECONDS).until {
            userEntity = userRepository.findByIdOrNull(user.id)
            userEntity != null && userEntity?.emailComplaint == true
        }

        assertNotNull(userEntity)
        assertTrue(userEntity!!.emailComplaint)
        assertFalse(userEntity!!.emailBounced)
    }

    @Test
    fun `handle async event - REGISTER_USER - success`() {
        val userId = Random.nextLong()
        val eventData =
            mapOf(
                "userId" to userId,
                "userEmail" to "ribbontek+${UUID.randomUUID()}@gmail.com",
                "userFirstName" to "TEST USER",
                "userUnsubscribeCode" to "123456"
            )
        val asyncDomainEvent = asyncDomainEvent {
            this.type = REGISTER_USER
            this.data = eventData.toJson()
        }

        asyncQueueMessagingTemplate.convertAndSend(asyncDomainEvent.toBase64String())
        var emailEntity: EmailEntity? = null
        await().pollInSameThread().atMost(10, SECONDS).pollInterval(1, SECONDS).until {
            emailEntity = emailRepository.findByUserIdAndTemplateName(userId, WELCOME_EMAIL.name)
            emailEntity != null && emailEntity?.status != IN_PROGRESS
        }

        assertNotNull(emailEntity)
        assertThat(emailEntity!!.status, equalTo(EmailStatusEnum.SUCCESS))
        assertThat(emailEntity!!.fromEmail, equalTo(sesClientConfig.senderEmail))
        assertThat(emailEntity!!.toEmail, equalTo(eventData["userEmail"]))
        assertThat(emailEntity!!.templateName, equalTo(WELCOME_EMAIL.name))
        assertNotNull(emailEntity!!.messageId)
    }

    @Test
    fun `handle async event - UNRECOGNIZED - throws exception`() {
        val asyncDomainEvent = asyncDomainEvent {
            typeValue = -1
            data = RandomStringUtils.randomAlphabetic(1000)
        }
        val exception = assertThrows<IllegalStateException> { eventAsyncQueueListener.handleMessage(asyncDomainEvent.toBase64String()) }
        assertThat(exception.message, equalTo("Invalid type for async domain event: ${asyncDomainEvent.type}"))
    }
}
