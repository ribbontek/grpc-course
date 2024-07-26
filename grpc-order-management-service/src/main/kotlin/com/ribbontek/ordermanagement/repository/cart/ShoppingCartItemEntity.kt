package com.ribbontek.ordermanagement.repository.cart

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

@Entity
@Table(name = "shopping_cart_item")
@AttributeOverride(name = "id", column = Column(name = "shopping_cart_item_id"))
class ShoppingCartItemEntity(
    @JsonIgnore
    @JoinColumn(name = "shopping_cart_session_id", referencedColumnName = "shopping_cart_session_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val session: ShoppingCartSessionEntity? = null,
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    @ManyToOne(optional = false)
    val product: ProductEntity,
    @Column(nullable = false)
    var quantity: Int
) : AbstractEntity()
