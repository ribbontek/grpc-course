package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.cart.ShoppingCartItemEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartStatus
import com.ribbontek.ordermanagement.repository.user.UserEntity
import java.math.BigDecimal
import java.util.UUID

object ShoppingCartSessionEntityFactory {
    fun create(
        userEntity: UserEntity,
        cartItems: List<ShoppingCartItemEntity>? = null
    ): ShoppingCartSessionEntity {
        return ShoppingCartSessionEntity(
            user = userEntity,
            sessionId = UUID.randomUUID(),
            total = cartItems?.sumOf { (it.quantity.toBigDecimal() * it.product.price) } ?: BigDecimal.ZERO,
            status = if (cartItems?.let { it.size > 1 } == true) ShoppingCartStatus.MODIFIED.name else ShoppingCartStatus.STARTED.name,
            cartItems = cartItems?.toMutableSet()
        )
    }
}
