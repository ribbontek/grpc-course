package com.ribbontek.ordermanagement.factory

import com.google.protobuf.Any
import com.ribbontek.grpccourse.Direction
import com.ribbontek.grpccourse.Direction.ASC
import com.ribbontek.grpccourse.Direction.DESC
import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.Sort
import com.ribbontek.grpccourse.pagingRequest
import com.ribbontek.grpccourse.productPagingQuery
import com.ribbontek.grpccourse.sort

object ProductPagingRequestFactory {
    fun products(
        size: Int = 100,
        sorted: Sort? = null,
        searchFor: String? = null,
        categoryCodeLookup: String? = null
    ): PagingRequest {
        return pagingRequest {
            this.number = 0
            this.size = size
            sorted?.let { this.sorted = sorted }
            this.query = Any.pack(
                productPagingQuery {
                    searchFor?.let { this.search = it }
                    categoryCodeLookup?.let { this.categoryCode = it }
                }
            )
        }
    }

    fun sorted(
        sortFields: List<String> = listOf("createdAt"),
        direction: Direction = listOf(ASC, DESC).random()
    ): Sort =
        sort {
            this.direction = direction
            this.properties.addAll(sortFields)
        }
}
