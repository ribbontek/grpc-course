package com.ribbontek.ordermanagement.service.product

import com.google.protobuf.Any
import com.ribbontek.grpccourse.PagingResponse
import com.ribbontek.grpccourse.Product
import com.ribbontek.grpccourse.pagingResponse
import com.ribbontek.ordermanagement.grpc.model.PagingRequestModel
import com.ribbontek.ordermanagement.grpc.model.ProductPagingQueryModel
import com.ribbontek.ordermanagement.mapping.toPageable
import com.ribbontek.ordermanagement.mapping.toProduct
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntitySpecs.hasCategoryCode
import com.ribbontek.ordermanagement.repository.product.ProductEntitySpecs.hasKeywordIn
import com.ribbontek.ordermanagement.repository.product.ProductEntitySpecs.notDeleted
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.shared.specdsl.and
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated

@Validated
interface ProductService {
    fun getProducts(): Flow<Product>

    fun getPagedProducts(
        @Valid pagingRequestModel: PagingRequestModel<ProductPagingQueryModel>
    ): PagingResponse
}

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository
) : ProductService {
    @Transactional(readOnly = true)
    override fun getProducts(): Flow<Product> {
        return productRepository.findAll().map { it.toProduct() }.asFlow()
    }

    @Transactional(readOnly = true)
    override fun getPagedProducts(pagingRequestModel: PagingRequestModel<ProductPagingQueryModel>): PagingResponse {
        return productRepository.findAll(pagingRequestModel.toSpec(), pagingRequestModel.toPageable()).toPagingResponse()
    }

    private fun Page<ProductEntity>.toPagingResponse(): PagingResponse {
        val page = this
        return pagingResponse {
            this.content.addAll(page.content.map { Any.pack(it.toProduct()) })
            this.totalPages = page.totalPages
            this.totalElements = page.totalElements
            this.size = page.size
            this.number = page.number
            this.numberOfElements = page.numberOfElements
            this.sorted = page.sort.isSorted
        }
    }

    private fun PagingRequestModel<ProductPagingQueryModel>.toSpec(): Specification<ProductEntity> {
        return this.query?.let {
            and(
                hasCategoryCode(it.categoryCode),
                hasKeywordIn(it.search?.split(" ")),
                notDeleted()
            )
        } ?: and(notDeleted())
    }
}
