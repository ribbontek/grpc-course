package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.order.OrderEntity
import com.ribbontek.ordermanagement.repository.order.OrderItemEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object OrderItemEntityFactory {
    fun create(
        session: OrderEntity,
        product: ProductEntity
    ): OrderItemEntity {
        return OrderItemEntity(
            order = session,
            product = product,
            price = FakerUtil.price(),
            quantity = FakerUtil.quantity()
        )
    }
}
