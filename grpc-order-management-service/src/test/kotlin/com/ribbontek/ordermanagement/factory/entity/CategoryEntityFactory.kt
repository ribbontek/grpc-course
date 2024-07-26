package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.product.CategoryEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object CategoryEntityFactory {
    fun create(): CategoryEntity {
        return CategoryEntity().apply {
            code = FakerUtil.alphanumeric(50)
            description = FakerUtil.alphanumeric(1000)
        }
    }
}
