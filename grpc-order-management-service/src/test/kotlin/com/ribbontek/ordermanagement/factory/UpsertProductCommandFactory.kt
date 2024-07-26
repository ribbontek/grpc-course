package com.ribbontek.ordermanagement.factory

import com.ribbontek.grpccourse.admin.UpsertProductCommand
import com.ribbontek.grpccourse.admin.upsertProductCommand
import com.ribbontek.ordermanagement.repository.product.CategoryEnum
import com.ribbontek.ordermanagement.util.FakerUtil
import java.util.UUID

object UpsertProductCommandFactory {
    fun create(
        requestId: String? = null,
        discountCode: String? = null
    ): UpsertProductCommand {
        return upsertProductCommand {
            this.requestId = requestId ?: UUID.randomUUID().toString()
            discountCode?.let { this.discountCode = it }
            categoryCode = FakerUtil.enum<CategoryEnum>().toString()
            title = FakerUtil.alphanumeric(255)
            description = FakerUtil.alphanumeric(1000)
            price = FakerUtil.price().toFloat()
            quantity = FakerUtil.quantity().toLong()
            sku = FakerUtil.alphanumeric(255)
        }
    }
}
