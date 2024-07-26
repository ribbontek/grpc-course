package com.ribbontek.ordermanagement.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.stereotype.Component

@Component
object DomainEventPublisher : ApplicationEventPublisherAware {
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher
    }

    fun publishEvent(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
