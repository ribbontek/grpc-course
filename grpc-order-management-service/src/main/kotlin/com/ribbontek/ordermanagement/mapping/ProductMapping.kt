package com.ribbontek.ordermanagement.mapping

import com.ribbontek.grpccourse.Product
import com.ribbontek.grpccourse.product
import com.ribbontek.ordermanagement.repository.product.ProductEntity

fun ProductEntity.toProduct(): Product {
    val source = this
    return product {
        this.requestId = source.requestId.toString()
        source.discount?.code?.let { this.discountCode = it }
        this.categoryCode = source.category.code!!
        this.title = source.title
        this.description = source.description
        this.quantity = source.quantity
        this.price = source.price.toFloat()
        source.sku?.let { this.sku = it }
    }
}
