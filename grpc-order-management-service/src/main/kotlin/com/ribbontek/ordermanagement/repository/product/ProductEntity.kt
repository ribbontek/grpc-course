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
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "product")
@AttributeOverride(name = "id", column = Column(name = "product_id"))
@SQLDelete(sql = "update product set deleted = true where product_id = ?")
class ProductEntity(
    @Column(nullable = false, unique = true, name = "product_uuid")
    val requestId: UUID,
    @JoinColumn(name = "discount_id", referencedColumnName = "discount_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var discount: DiscountEntity? = null,
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", nullable = false)
    @ManyToOne(optional = false)
    var category: CategoryEntity,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(nullable = false, length = 1000)
    var description: String,
    @Column(nullable = false)
    var quantity: Long,
    @Column(nullable = false)
    var price: BigDecimal,
    @Column(nullable = false, length = 255)
    var sku: String? = null
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
