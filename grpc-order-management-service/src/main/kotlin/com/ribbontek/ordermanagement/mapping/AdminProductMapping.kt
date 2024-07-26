package com.ribbontek.ordermanagement.mapping

import com.ribbontek.grpccourse.admin.AdminCategory
import com.ribbontek.grpccourse.admin.AdminDiscount
import com.ribbontek.grpccourse.admin.AdminProduct
import com.ribbontek.grpccourse.admin.UpsertCategoryCommand
import com.ribbontek.grpccourse.admin.UpsertDiscountCommand
import com.ribbontek.grpccourse.admin.UpsertProductCommand
import com.ribbontek.grpccourse.admin.adminCategory
import com.ribbontek.grpccourse.admin.adminDiscount
import com.ribbontek.grpccourse.admin.adminProduct
import com.ribbontek.ordermanagement.grpc.model.UpsertCategoryCommandModel
import com.ribbontek.ordermanagement.grpc.model.UpsertDiscountCommandModel
import com.ribbontek.ordermanagement.grpc.model.UpsertProductCommandModel
import com.ribbontek.ordermanagement.repository.product.CategoryEntity
import com.ribbontek.ordermanagement.repository.product.DiscountEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.shared.util.toUtc
import java.time.ZonedDateTime

fun UpsertDiscountCommand.toUpsertDiscountCommandModel(): UpsertDiscountCommandModel {
    return UpsertDiscountCommandModel(
        amount = amount.toBigDecimal(),
        code = code,
        description = description,
        expiresAtUtc = ZonedDateTime.parse(expiresAtUtc).toUtc()
    )
}

fun UpsertCategoryCommand.toUpsertCategoryCommandModel(): UpsertCategoryCommandModel {
    return UpsertCategoryCommandModel(
        code = code,
        description = description
    )
}

fun UpsertProductCommand.toUpsertProductCommandModel(): UpsertProductCommandModel {
    return UpsertProductCommandModel(
        requestId = requestId,
        discountCode = if (hasDiscountCode()) discountCode else null,
        categoryCode = categoryCode,
        title = title,
        description = description,
        price = price.toBigDecimal(),
        quantity = quantity,
        sku = if (hasSku()) sku else null
    )
}

fun DiscountEntity.toDiscount(): AdminDiscount {
    val source = this
    return adminDiscount {
        this.code = source.code
        this.amount = source.amount.toFloat()
        this.expiresAtUtc = source.expiresAt!!.toUtc().toString()
        this.createdAtUtc = source.createdAt!!.toUtc().toString()
        this.createdBy = source.createdBy!!
        source.modifiedBy?.let { this.modifiedBy = it }
        source.modifiedAt?.let { this.modifiedAtUtc = it.toUtc().toString() }
    }
}

fun CategoryEntity.toCategory(): AdminCategory {
    val source = this
    return adminCategory {
        this.code = source.code!!
        this.description = source.description!!
        this.createdAtUtc = source.createdAt!!.toUtc().toString()
        source.modifiedAt?.let { this.modifiedAtUtc = it.toUtc().toString() }
    }
}

fun ProductEntity.toAdminProduct(): AdminProduct {
    val source = this
    return adminProduct {
        this.requestId = source.requestId.toString()
        source.discount?.code?.let { this.discountCode = it }
        this.categoryCode = source.category.code!!
        this.title = source.title
        this.description = source.description
        this.quantity = source.quantity
        this.price = source.price.toFloat()
        source.sku?.let { this.sku = it }
        this.createdAtUtc = source.createdAt!!.toUtc().toString()
        this.createdBy = source.createdBy!!
        source.modifiedBy?.let { this.modifiedBy = it }
        source.modifiedAt?.let { this.modifiedAtUtc = it.toUtc().toString() }
    }
}

fun DiscountEntity.toAdminDiscount(): AdminDiscount {
    val source = this
    return adminDiscount {
        this.amount = source.amount.toFloat()
        this.code = source.code
        this.expiresAtUtc = source.expiresAt?.toUtc().toString()
        this.createdAtUtc = source.createdAt!!.toUtc().toString()
        this.createdBy = source.createdBy!!
        source.modifiedBy?.let { this.modifiedBy = it }
        source.modifiedAt?.let { this.modifiedAtUtc = it.toUtc().toString() }
    }
}

fun CategoryEntity.toAdminCategory(): AdminCategory {
    val source = this
    return adminCategory {
        this.code = source.code!!
        this.description = source.description!!
        this.createdAtUtc = source.createdAt!!.toUtc().toString()
        source.modifiedAt?.let { this.modifiedAtUtc = it.toUtc().toString() }
    }
}
