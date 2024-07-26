package com.ribbontek.ordermanagement.repository.order

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
import com.ribbontek.ordermanagement.repository.user.AddressTypeEntity
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.service.DomainEventPublisher
import com.ribbontek.ordermanagement.service.event.AuditEventType.CREATE
import com.ribbontek.ordermanagement.service.event.AuditEventType.DELETE
import com.ribbontek.ordermanagement.service.event.AuditEventType.UPDATE
import com.ribbontek.ordermanagement.service.event.Auditable
import com.ribbontek.ordermanagement.service.event.toAuditEvent
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapKeyJoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

enum class OrderStatus {
    MODIFIED,
    CANCELLED,
    CONFIRMED,
    DELIVERED
}

@Entity
@Table(name = "order")
@AttributeOverride(name = "id", column = Column(name = "order_id"))
class OrderEntity(
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val user: UserEntity? = null,
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id", nullable = true)
    @ManyToOne(optional = true, fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var payment: PaymentEntity? = null,
    @Column(nullable = false, unique = true, name = "order_session_uuid")
    val sessionId: UUID,
    @Column(nullable = false)
    val total: BigDecimal,
    @Column(nullable = false, length = 100)
    var status: String,
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "order", fetch = FetchType.LAZY)
    var orderItems: MutableSet<OrderItemEntity>? = null,
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @MapKeyJoinColumn(name = "address_type_id")
    var addresses: MutableMap<AddressTypeEntity, OrderAddressEntity>? = null
) : AbstractEntity(), Auditable {
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
