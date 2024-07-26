package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.product.DiscountEntity
import com.ribbontek.ordermanagement.util.FakerUtil
import java.time.ZonedDateTime

object DiscountEntityFactory {
    fun create(): DiscountEntity {
        return DiscountEntity(
            amount = FakerUtil.price(),
            code = FakerUtil.alphanumeric(50),
            description = FakerUtil.alphanumeric(),
            expiresAt = ZonedDateTime.now().plusDays(3)
        )
    }
}
