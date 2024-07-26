package com.ribbontek.ordermanagement.service.event

import com.ribbontek.ordermanagement.service.DomainEvent

interface EventHandler<T : DomainEvent> {
    fun listen(event: T)
}
