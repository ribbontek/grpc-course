package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.cart.ShoppingCartItemEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object ShoppingCartItemEntityFactory {
    fun create(
        productEntity: ProductEntity,
        quantity: Int? = null,
        session: ShoppingCartSessionEntity? = null
    ): ShoppingCartItemEntity {
        return ShoppingCartItemEntity(
            session = session,
            product = productEntity,
            quantity = quantity ?: FakerUtil.quantity()
        )
    }
}
