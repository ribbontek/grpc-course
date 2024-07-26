package com.ribbontek.ordermanagement.mapping

import com.google.protobuf.InvalidProtocolBufferException
import com.ribbontek.grpccourse.Direction.DESC
import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.ProductPagingQuery
import com.ribbontek.ordermanagement.grpc.model.PagingQuery
import com.ribbontek.ordermanagement.grpc.model.PagingRequestModel
import com.ribbontek.ordermanagement.grpc.model.ProductPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.Sorted
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

fun PagingRequest.toProductPagingRequestModel(): PagingRequestModel<ProductPagingQueryModel> {
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
                    source.query.unpack(ProductPagingQuery::class.java)
                } catch (ex: InvalidProtocolBufferException) {
                    null
                }?.toProductPagingQueryModel()
            else -> null
        }
    )
}

fun <T : PagingQuery> PagingRequestModel<T>.toPageable(): PageRequest {
    return this.sorted?.let {
        PageRequest.of(
            this.number,
            this.size,
            Sort.by(if (it.direction == DESC) Sort.Direction.DESC else Sort.Direction.ASC, *it.properties.toTypedArray())
        )
    } ?: PageRequest.of(this.number, this.size)
}

private fun ProductPagingQuery.toProductPagingQueryModel(): ProductPagingQueryModel {
    val source = this
    return ProductPagingQueryModel(
        categoryCode = if (source.hasCategoryCode()) source.categoryCode else null,
        search = if (source.hasSearch()) source.search else null
    )
}
