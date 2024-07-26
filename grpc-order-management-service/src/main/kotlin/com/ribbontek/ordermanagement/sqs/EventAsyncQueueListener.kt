package com.ribbontek.ordermanagement.sqs

import com.ribbontek.grpccourse.event.AsyncDomainEvent
import com.ribbontek.ordermanagement.sqs.handler.AsyncEventHandler
import com.ribbontek.ordermanagement.util.decodeBase64
import com.ribbontek.shared.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class EventAsyncQueueListener(
    private val asyncEventHandlers: List<AsyncEventHandler>
) {
    private val log = logger()

    @ConditionalOnProperty(value = ["sqs.queue.processor.enabled"], havingValue = "true")
    @SqsListener("\${sqs.queue.processor.async.uri}", deletionPolicy = ON_SUCCESS)
    fun handleMessage(message: String) {
        log.info("Received async event message: $message")
        val asyncDomainEvent = AsyncDomainEvent.parseFrom(message.decodeBase64())
        when (asyncEventHandlers.any { it.type == asyncDomainEvent.type }) {
            true -> asyncEventHandlers.filter { it.type == asyncDomainEvent.type }.forEach { it.handleEvent(asyncDomainEvent) }
            false -> throw IllegalStateException("Invalid type for async domain event: ${asyncDomainEvent.type}")
        }
    }
}
