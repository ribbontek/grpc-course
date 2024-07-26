package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.order.OrderEntity
import com.ribbontek.ordermanagement.repository.order.OrderItemEntity
import com.ribbontek.ordermanagement.repository.order.PaymentEntity
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.util.FakerUtil
import java.util.UUID

object OrderSessionEntityFactory {
    fun create(
        userEntity: UserEntity,
        paymentEntity: PaymentEntity? = null,
        orderItems: List<OrderItemEntity>? = null
    ): OrderEntity {
        return OrderEntity(
            user = userEntity,
            payment = paymentEntity,
            sessionId = UUID.randomUUID(),
            total = FakerUtil.price(),
            status = FakerUtil.alphanumeric(50),
            orderItems = orderItems?.toMutableSet()
        )
    }
}
