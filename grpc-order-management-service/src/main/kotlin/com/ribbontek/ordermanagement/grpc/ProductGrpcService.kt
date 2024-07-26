package com.ribbontek.ordermanagement.grpc

import com.google.protobuf.Empty
import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.PagingResponse
import com.ribbontek.grpccourse.Product
import com.ribbontek.grpccourse.ProductServiceGrpcKt.ProductServiceCoroutineImplBase
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toProductPagingRequestModel
import com.ribbontek.ordermanagement.service.product.ProductService
import kotlinx.coroutines.flow.Flow

@RibbontekGrpcService
class ProductGrpcService(
    private val productService: ProductService
) : ProductServiceCoroutineImplBase() {

    override fun getProducts(request: Empty): Flow<Product> {
        return productService.getProducts()
    }

    override suspend fun getPagedProducts(request: PagingRequest): PagingResponse {
        return productService.getPagedProducts(request.toProductPagingRequestModel())
    }
}
