package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.order.PaymentEntity
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object PaymentEntityFactory {
    fun create(userEntity: UserEntity): PaymentEntity {
        return PaymentEntity(
            user = userEntity,
            amount = FakerUtil.price(),
            provider = FakerUtil.alphanumeric(100),
            reference = FakerUtil.alphanumeric()
        )
    }
}
