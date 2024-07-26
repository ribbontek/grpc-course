package com.ribbontek.ordermanagement.repository.cart

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
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
import jakarta.persistence.OneToMany
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

enum class ShoppingCartStatus {
    STARTED,
    MODIFIED,
    CANCELLED,
    CONVERTED
}

@Entity
@Table(name = "shopping_cart_session")
@AttributeOverride(name = "id", column = Column(name = "shopping_cart_session_id"))
class ShoppingCartSessionEntity(
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val user: UserEntity? = null,
    @Column(nullable = false, unique = true, name = "shopping_cart_session_uuid")
    val sessionId: UUID,
    @Column(nullable = false)
    var total: BigDecimal,
    @Column(nullable = false, length = 100)
    var status: String,
    @OneToMany(
        cascade = [CascadeType.ALL],
        mappedBy = "session",
        fetch = FetchType.LAZY
    )
    var cartItems: MutableSet<ShoppingCartItemEntity>? = null
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
