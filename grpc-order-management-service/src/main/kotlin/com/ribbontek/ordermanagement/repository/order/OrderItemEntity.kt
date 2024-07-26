package com.ribbontek.ordermanagement.repository.order

import com.fasterxml.jackson.annotation.JsonIgnore
import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_item")
@AttributeOverride(name = "id", column = Column(name = "order_item_id"))
class OrderItemEntity(
    @JsonIgnore
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val order: OrderEntity? = null,
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val product: ProductEntity? = null,
    @Column(nullable = false)
    val price: BigDecimal,
    @Column(nullable = false)
    val quantity: Int
) : AbstractEntity()
