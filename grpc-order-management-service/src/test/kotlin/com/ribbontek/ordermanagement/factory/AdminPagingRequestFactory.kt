package com.ribbontek.ordermanagement.factory

import com.google.protobuf.Any
import com.ribbontek.grpccourse.Direction
import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.Sort
import com.ribbontek.grpccourse.admin.adminCategoryPagingQuery
import com.ribbontek.grpccourse.admin.adminDiscountPagingQuery
import com.ribbontek.grpccourse.admin.adminProductPagingQuery
import com.ribbontek.grpccourse.pagingRequest
import com.ribbontek.grpccourse.sort
import com.ribbontek.ordermanagement.util.FakerUtil
import java.math.BigDecimal

object AdminPagingRequestFactory {
    fun adminProducts(
        sorted: Sort? = null,
        titleLike: String? = null,
        descriptionLike: String? = null,
        deleted: Boolean? = null,
        hasDiscount: Boolean? = null,
        hasStock: Boolean? = null,
        priceRangeStart: BigDecimal? = null,
        priceRangeEnd: BigDecimal? = null
    ): PagingRequest {
        return pagingRequest {
            number = 0
            size = 100
            sorted?.let { this.sorted = sorted }
            query =
                Any.pack(
                    adminProductPagingQuery {
                        titleLike?.let { this.titleLike = it }
                        descriptionLike?.let { this.descriptionLike = it }
                        deleted?.let { this.deleted = it }
                        hasDiscount?.let { this.hasDiscount = it }
                        hasStock?.let { this.hasStock = it }
                        priceRangeStart?.let { this.priceRangeStart = it.toInt() }
                        priceRangeEnd?.let { this.priceRangeEnd = it.toInt() }
                    }
                )
        }
    }

    fun adminDiscounts(
        sorted: Sort? = null,
        codeLike: String? = null,
        amountRangeStart: BigDecimal? = null,
        amountRangeEnd: BigDecimal? = null,
        expired: Boolean? = null,
        deleted: Boolean? = null
    ): PagingRequest {
        return pagingRequest {
            number = 0
            size = 100
            sorted?.let { this.sorted = sorted }
            query =
                Any.pack(
                    adminDiscountPagingQuery {
                        codeLike?.let { this.codeLike = it }
                        deleted?.let { this.deleted = it }
                        expired?.let { this.expired = it }
                        amountRangeStart?.let { this.amountRangeStart = it.toInt() }
                        amountRangeEnd?.let { this.amountRangeEnd = it.toInt() }
                    }
                )
        }
    }

    fun adminCategories(
        sorted: Sort? = null,
        codeLike: String? = null,
        descriptionLike: String? = null
    ): PagingRequest {
        return pagingRequest {
            number = 0
            size = 100
            sorted?.let { this.sorted = sorted }
            query =
                Any.pack(
                    adminCategoryPagingQuery {
                        codeLike?.let { this.codeLike = it }
                        descriptionLike?.let { this.descriptionLike = it }
                    }
                )
        }
    }

    fun sorted(sortFields: List<String> = listOf("createdAt")): Sort =
        sort {
            direction = FakerUtil.enum<Direction>()
            properties.addAll(sortFields)
        }
}
