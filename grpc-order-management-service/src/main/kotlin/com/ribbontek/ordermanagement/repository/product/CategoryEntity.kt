package com.ribbontek.ordermanagement.repository.product

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntityCodeDescription
import com.ribbontek.ordermanagement.service.DomainEventPublisher
import com.ribbontek.ordermanagement.service.event.AuditEventType.CREATE
import com.ribbontek.ordermanagement.service.event.AuditEventType.DELETE
import com.ribbontek.ordermanagement.service.event.AuditEventType.UPDATE
import com.ribbontek.ordermanagement.service.event.Auditable
import com.ribbontek.ordermanagement.service.event.toAuditEvent
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table

enum class CategoryEnum {
    CLOTHING,
    FOOTWEAR,
    COMPUTERS,
    COMPUTERS_SOFTWARE,
    COMPUTERS_NETWORKING
}

@Entity
@Table(name = "category")
@AttributeOverride(name = "id", column = Column(name = "category_id"))
class CategoryEntity() : AbstractEntityCodeDescription(), Auditable {
    // creation via enums though not a guarantee of working if a user eventually deletes category
    constructor(categoryEnum: CategoryEnum) : this() {
        code = categoryEnum.name
    }

    @PostPersist
    override fun postPersist() {
        DomainEventPublisher.publishEvent(this.toAuditEvent(CREATE))
    }

    @PostUpdate
    override fun postUpdate() {
        DomainEventPublisher.publishEvent(this.toAuditEvent(UPDATE))
    }

    @PostRemove
    override fun postRemove() {
        DomainEventPublisher.publishEvent(this.toAuditEvent(DELETE))
    }
}
