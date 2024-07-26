package com.ribbontek.ordermanagement.service.event.handler

import com.ribbontek.grpccourse.event.EventType.REGISTER_USER
import com.ribbontek.grpccourse.event.asyncDomainEvent
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneById
import com.ribbontek.ordermanagement.service.RegisterUserEvent
import com.ribbontek.ordermanagement.service.event.EventHandler
import com.ribbontek.ordermanagement.util.toBase64String
import com.ribbontek.shared.util.logger
import com.ribbontek.shared.util.toJson
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("!integration")
class RegisterUserEventHandler(
    private val userRepository: UserRepository,
    private val asyncQueueMessagingTemplate: QueueMessagingTemplate
) : EventHandler<RegisterUserEvent> {
    private val log = logger()

    @Async
    @EventListener
    override fun listen(event: RegisterUserEvent) {
        log.info("processing event $event")
        val user = userRepository.expectOneById(event.userId)
        val code = UUID.randomUUID().toString().substring(0, 8)
        userRepository.saveAndFlush(user.apply { unsubscribeCode = code })
        val eventData = mapOf(
            "userId" to user.id.toString(),
            "userEmail" to user.email,
            "userFirstName" to user.firstName,
            "userUnsubscribeCode" to code
        )
        val asyncDomainEvent = asyncDomainEvent {
            this.type = REGISTER_USER
            this.data = eventData.toJson()
        }
        asyncQueueMessagingTemplate.convertAndSend(asyncDomainEvent.toBase64String())
    }
}
