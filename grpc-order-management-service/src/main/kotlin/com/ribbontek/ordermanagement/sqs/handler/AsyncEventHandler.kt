package com.ribbontek.ordermanagement.sqs.handler

import com.ribbontek.grpccourse.event.AsyncDomainEvent
import com.ribbontek.grpccourse.event.EventType
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger

abstract class AsyncEventHandler {
    protected val log = logger()

    abstract val type: EventType

    protected abstract fun handleEventImpl(event: AsyncDomainEvent)

    fun handleEvent(event: AsyncDomainEvent) {
        tryRun {
            log.info("Processing event $type with ${this.javaClass.simpleName}")
            handleEventImpl(event)
        }.onSuccess {
            log.info("Successfully processed event $type with ${this.javaClass.simpleName}")
        }.onFailure {
            log.info("Failure processing event $type with ${this.javaClass.simpleName}", it)
        }
    }
}
