package com.ribbontek.ordermanagement.repository.product

import com.ribbontek.ordermanagement.repository.abstracts.AbstractAdminEntityDelete
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
import org.hibernate.annotations.SQLDelete
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "discount")
@AttributeOverride(name = "id", column = Column(name = "discount_id"))
@SQLDelete(sql = "update discount set deleted = true where discount_id = ?")
class DiscountEntity(
    @Column(nullable = false)
    var amount: BigDecimal,
    @Column(nullable = false, length = 50)
    var code: String,
    @Column(length = 255)
    var description: String? = null,
    @Column
    var expiresAt: ZonedDateTime? = null
) : AbstractAdminEntityDelete(), Auditable {
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
