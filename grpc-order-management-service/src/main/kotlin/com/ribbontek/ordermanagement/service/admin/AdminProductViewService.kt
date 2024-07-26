package com.ribbontek.ordermanagement.service.admin

import com.google.protobuf.Any
import com.ribbontek.grpccourse.PagingResponse
import com.ribbontek.grpccourse.pagingResponse
import com.ribbontek.ordermanagement.grpc.model.AdminCategoryPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.AdminDiscountPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.AdminProductPagingQueryModel
import com.ribbontek.ordermanagement.grpc.model.PagingRequestModel
import com.ribbontek.ordermanagement.mapping.toAdminCategory
import com.ribbontek.ordermanagement.mapping.toAdminDiscount
import com.ribbontek.ordermanagement.mapping.toAdminProduct
import com.ribbontek.ordermanagement.mapping.toPageable
import com.ribbontek.ordermanagement.repository.product.CategoryEntity
import com.ribbontek.ordermanagement.repository.product.CategoryEntitySpecs
import com.ribbontek.ordermanagement.repository.product.CategoryRepository
import com.ribbontek.ordermanagement.repository.product.DiscountEntity
import com.ribbontek.ordermanagement.repository.product.DiscountEntitySpecs
import com.ribbontek.ordermanagement.repository.product.DiscountRepository
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductEntitySpecs
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.shared.specdsl.and
import com.ribbontek.shared.specdsl.emptySpecification
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated

@Validated
interface AdminProductViewService {
    fun getPagedProducts(
        @Valid pageRequest: PagingRequestModel<AdminProductPagingQueryModel>
    ): PagingResponse

    fun getPagedDiscounts(
        @Valid pageRequest: PagingRequestModel<AdminDiscountPagingQueryModel>
    ): PagingResponse

    fun getPagedCategories(
        @Valid pageRequest: PagingRequestModel<AdminCategoryPagingQueryModel>
    ): PagingResponse
}

@Service
class AdminProductViewServiceImpl(
    private val productRepository: ProductRepository,
    private val discountRepository: DiscountRepository,
    private val categoryRepository: CategoryRepository
) : AdminProductViewService {
    @Transactional(readOnly = true)
    override fun getPagedProducts(pageRequest: PagingRequestModel<AdminProductPagingQueryModel>): PagingResponse {
        return productRepository.findAll(pageRequest.toProductSpec(), pageRequest.toPageable()).toAdminProductPagingResponse()
    }

    @Transactional(readOnly = true)
    override fun getPagedDiscounts(
        @Valid pageRequest: PagingRequestModel<AdminDiscountPagingQueryModel>
    ): PagingResponse {
        return discountRepository.findAll(pageRequest.toDiscountSpec(), pageRequest.toPageable()).toAdminDiscountPagingResponse()
    }

    @Transactional(readOnly = true)
    override fun getPagedCategories(
        @Valid pageRequest: PagingRequestModel<AdminCategoryPagingQueryModel>
    ): PagingResponse {
        return categoryRepository.findAll(pageRequest.toCategorySpec(), pageRequest.toPageable()).toAdminCategoryPagingResponse()
    }

    private fun PagingRequestModel<AdminCategoryPagingQueryModel>.toCategorySpec(): Specification<CategoryEntity> {
        return this.query?.let {
            and(
                CategoryEntitySpecs.hasCodeLike(it.codeLike),
                CategoryEntitySpecs.hasDescriptionLike(it.descriptionLike)
            )
        } ?: emptySpecification()
    }

    private fun PagingRequestModel<AdminDiscountPagingQueryModel>.toDiscountSpec(): Specification<DiscountEntity> {
        return this.query?.let {
            and(
                DiscountEntitySpecs.hasCodeLike(it.codeLike),
                DiscountEntitySpecs.hasAmountRangeStart(it.amountRangeStart),
                DiscountEntitySpecs.hasAmountRangeEnd(it.amountRangeEnd),
                DiscountEntitySpecs.hasDeleted(it.deleted),
                DiscountEntitySpecs.hasExpired(it.expired)
            )
        } ?: emptySpecification()
    }

    private fun PagingRequestModel<AdminProductPagingQueryModel>.toProductSpec(): Specification<ProductEntity> {
        return this.query?.let {
            and(
                ProductEntitySpecs.hasTitleLike(it.titleLike),
                ProductEntitySpecs.hasDescriptionLike(it.descriptionLike),
                ProductEntitySpecs.hasDeleted(it.deleted),
                ProductEntitySpecs.hasDiscount(it.hasDiscount),
                ProductEntitySpecs.hasStock(it.hasStock),
                ProductEntitySpecs.hasPriceRangeStart(it.priceRangeStart),
                ProductEntitySpecs.hasPriceRangeEnd(it.priceRangeEnd)
            )
        } ?: emptySpecification()
    }

    private fun Page<ProductEntity>.toAdminProductPagingResponse(): PagingResponse {
        val page = this
        return pagingResponse {
            this.content.addAll(page.content.map { Any.pack(it.toAdminProduct()) })
            this.totalPages = page.totalPages
            this.totalElements = page.totalElements
            this.size = page.size
            this.number = page.number
            this.numberOfElements = page.numberOfElements
            this.sorted = page.sort.isSorted
        }
    }

    private fun Page<DiscountEntity>.toAdminDiscountPagingResponse(): PagingResponse {
        val page = this
        return pagingResponse {
            this.content.addAll(page.content.map { Any.pack(it.toAdminDiscount()) })
            this.totalPages = page.totalPages
            this.totalElements = page.totalElements
            this.size = page.size
            this.number = page.number
            this.numberOfElements = page.numberOfElements
            this.sorted = page.sort.isSorted
        }
    }

    private fun Page<CategoryEntity>.toAdminCategoryPagingResponse(): PagingResponse {
        val page = this
        return pagingResponse {
            this.content.addAll(page.content.map { Any.pack(it.toAdminCategory()) })
            this.totalPages = page.totalPages
            this.totalElements = page.totalElements
            this.size = page.size
            this.number = page.number
            this.numberOfElements = page.numberOfElements
            this.sorted = page.sort.isSorted
        }
    }
}
