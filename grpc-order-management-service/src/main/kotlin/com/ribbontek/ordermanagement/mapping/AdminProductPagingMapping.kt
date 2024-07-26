package com.ribbontek.ordermanagement.mapping

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.admin.AdminCategoryPagingQuery
import com.ribbontek.grpccourse.admin.AdminDiscountPagingQuery
import com.ribbontek.grpccourse.admin.AdminProductPagingQuery
import com.ribbontek.ordermanagement.grpc.model.AdminCategoryPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.AdminDiscountPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.AdminProductPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.PagingQuery
import com.ribbontek.ordermanagement.grpc.model.PagingRequestModel
import com.ribbontek.ordermanagement.grpc.model.Sorted

private inline fun <reified T : Message, R : PagingQuery> PagingRequest.toPagingRequestModel(query: T.() -> R): PagingRequestModel<R> {
    val source = this
    return PagingRequestModel(
        number = source.number,
        size = source.size,
        sorted =
        when {
            source.hasSorted() ->
                Sorted(
                    direction = source.sorted.direction,
                    properties = source.sorted.propertiesList
                )
            else -> null
        },
        query =
        when {
            source.hasQuery() ->
                try {
                    query(source.query.unpack(T::class.java))
                } catch (ex: InvalidProtocolBufferException) {
                    null
                }
            else -> null
        }
    )
}

fun PagingRequest.toAdminDiscountRequestModel(): PagingRequestModel<AdminDiscountPagingQueryModel> {
    return this.toPagingRequestModel<AdminDiscountPagingQuery, AdminDiscountPagingQueryModel> { toAdminDiscountPagingQueryModel() }
}

private fun AdminDiscountPagingQuery.toAdminDiscountPagingQueryModel(): AdminDiscountPagingQueryModel {
    val source = this
    return AdminDiscountPagingQueryModel(
        codeLike = if (source.hasCodeLike()) source.codeLike else null,
        amountRangeStart = if (source.hasAmountRangeStart()) source.amountRangeStart.toBigDecimal() else null,
        amountRangeEnd = if (source.hasAmountRangeEnd()) source.amountRangeEnd.toBigDecimal() else null,
        expired = if (source.hasExpired()) source.expired else null
    )
}

fun PagingRequest.toAdminProductRequestModel(): PagingRequestModel<AdminProductPagingQueryModel> {
    return this.toPagingRequestModel<AdminProductPagingQuery, AdminProductPagingQueryModel> { toAdminProductPagingQueryModel() }
}

private fun AdminProductPagingQuery.toAdminProductPagingQueryModel(): AdminProductPagingQueryModel {
    val source = this
    return AdminProductPagingQueryModel(
        titleLike = if (source.hasTitleLike()) source.titleLike else null,
        descriptionLike = if (source.hasDescriptionLike()) source.descriptionLike else null,
        deleted = if (source.hasDeleted()) source.deleted else null,
        hasDiscount = if (source.hasHasDiscount()) source.hasDiscount else null,
        hasStock = if (source.hasHasStock()) source.hasStock else null,
        priceRangeStart = if (source.hasPriceRangeStart()) source.priceRangeStart.toBigDecimal() else null,
        priceRangeEnd = if (source.hasPriceRangeEnd()) source.priceRangeEnd.toBigDecimal() else null
    )
}

fun PagingRequest.toAdminCategoryRequestModel(): PagingRequestModel<AdminCategoryPagingQueryModel> {
    return this.toPagingRequestModel<AdminCategoryPagingQuery, AdminCategoryPagingQueryModel> { toAdminCategoryPagingQueryModel() }
}

private fun AdminCategoryPagingQuery.toAdminCategoryPagingQueryModel(): AdminCategoryPagingQueryModel {
    val source = this
    return AdminCategoryPagingQueryModel(
        codeLike = if (source.hasCodeLike()) source.codeLike else null,
        descriptionLike = if (source.hasDescriptionLike()) source.descriptionLike else null
    )
}
