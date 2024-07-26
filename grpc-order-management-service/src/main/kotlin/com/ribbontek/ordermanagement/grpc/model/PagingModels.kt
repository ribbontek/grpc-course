package com.ribbontek.ordermanagement.grpc.model

import com.ribbontek.grpccourse.Direction
import java.math.BigDecimal

interface PagingQuery

data class Sorted(
    val direction: Direction,
    val properties: List<String>
)

// TODO: Add validation restrictions
data class PagingRequestModel<T : PagingQuery>(
    val number: Int,
    val size: Int,
    val sorted: Sorted? = null,
    val query: T? = null
)

data class ProductPagingQueryModel(
    val categoryCode: String? = null,
    val search: String? = null
) : PagingQuery

data class AdminDiscountPagingQueryModel(
    val codeLike: String? = null,
    val amountRangeStart: BigDecimal? = null,
    val amountRangeEnd: BigDecimal? = null,
    val expired: Boolean? = null,
    val deleted: Boolean? = null
) : PagingQuery

data class AdminCategoryPagingQueryModel(
    val codeLike: String? = null,
    val descriptionLike: String? = null
) : PagingQuery

data class AdminProductPagingQueryModel(
    val titleLike: String? = null,
    val descriptionLike: String? = null,
    val deleted: Boolean? = null,
    val hasDiscount: Boolean? = null,
    val hasStock: Boolean? = null,
    val priceRangeStart: BigDecimal? = null,
    val priceRangeEnd: BigDecimal? = null
) : PagingQuery
