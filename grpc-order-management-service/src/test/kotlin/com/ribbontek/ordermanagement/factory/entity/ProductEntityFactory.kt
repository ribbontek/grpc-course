package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.product.CategoryEntity
import com.ribbontek.ordermanagement.repository.product.DiscountEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.util.FakerUtil
import java.util.UUID
import kotlin.random.Random

object ProductEntityFactory {
    fun create(
        discount: DiscountEntity? = null,
        category: CategoryEntity? = null
    ): ProductEntity {
        return ProductEntity(
            requestId = UUID.randomUUID(),
            discount = discount,
            category = category ?: CategoryEntity(categoryEnum = FakerUtil.enum()),
            quantity = FakerUtil.quantity().toLong(),
            price = FakerUtil.price(),
            sku = FakerUtil.alphanumeric(),
            title = if (Random.nextBoolean()) "TEST-" + FakerUtil.alphanumeric(250) else FakerUtil.alphanumeric(),
            description = FakerUtil.alphanumeric(1000)
        )
    }
}
