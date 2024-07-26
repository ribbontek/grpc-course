package com.ribbontek.ordermanagement.service.admin

import com.ribbontek.grpccourse.admin.AdminCategory
import com.ribbontek.grpccourse.admin.AdminDiscount
import com.ribbontek.grpccourse.admin.AdminProduct
import com.ribbontek.ordermanagement.grpc.model.UpsertCategoryCommandModel
import com.ribbontek.ordermanagement.grpc.model.UpsertDiscountCommandModel
import com.ribbontek.ordermanagement.grpc.model.UpsertProductCommandModel
import com.ribbontek.ordermanagement.mapping.toAdminProduct
import com.ribbontek.ordermanagement.mapping.toCategory
import com.ribbontek.ordermanagement.mapping.toDiscount
import com.ribbontek.ordermanagement.repository.product.CategoryEntity
import com.ribbontek.ordermanagement.repository.product.CategoryRepository
import com.ribbontek.ordermanagement.repository.product.DiscountEntity
import com.ribbontek.ordermanagement.repository.product.DiscountRepository
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.ordermanagement.repository.product.expectOneByCode
import com.ribbontek.ordermanagement.util.toUUID
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated

@Validated
interface AdminProductUpsertService {
    fun upsertDiscount(
        @Valid cmd: UpsertDiscountCommandModel
    ): AdminDiscount

    fun upsertCategory(
        @Valid cmd: UpsertCategoryCommandModel
    ): AdminCategory

    fun upsertProduct(
        @Valid cmd: UpsertProductCommandModel
    ): AdminProduct
}

@Service
class AdminProductUpsertServiceImpl(
    private val discountRepository: DiscountRepository,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository
) : AdminProductUpsertService {
    @Transactional
    override fun upsertDiscount(cmd: UpsertDiscountCommandModel): AdminDiscount {
        return discountRepository.findByCode(cmd.code).upsert(cmd).toDiscount()
    }

    @Transactional
    override fun upsertCategory(cmd: UpsertCategoryCommandModel): AdminCategory {
        return categoryRepository.findByCode(cmd.code).upsert(cmd).toCategory()
    }

    @Transactional
    override fun upsertProduct(cmd: UpsertProductCommandModel): AdminProduct {
        return productRepository.findByRequestId(cmd.requestId.toUUID()).upsert(cmd).toAdminProduct()
    }

    private fun ProductEntity?.upsert(cmd: UpsertProductCommandModel): ProductEntity {
        return productRepository.saveAndFlush(
            this?.apply {
                discount = cmd.discountCode?.let { discountRepository.expectOneByCode(it) }
                category = categoryRepository.expectOneByCode(cmd.categoryCode)
                title = cmd.title
                description = cmd.description
                quantity = cmd.quantity
                price = cmd.price
                sku = cmd.sku
            } ?: ProductEntity(
                requestId = cmd.requestId.toUUID(),
                discount = cmd.discountCode?.let { discountRepository.expectOneByCode(it) },
                category = categoryRepository.expectOneByCode(cmd.categoryCode),
                title = cmd.title,
                description = cmd.description,
                quantity = cmd.quantity,
                price = cmd.price,
                sku = cmd.sku
            )
        )
    }

    private fun CategoryEntity?.upsert(cmd: UpsertCategoryCommandModel): CategoryEntity {
        return categoryRepository.saveAndFlush(
            this?.apply {
                description = cmd.description
            } ?: CategoryEntity().apply {
                code = cmd.code
                description = cmd.description
            }
        )
    }

    private fun DiscountEntity?.upsert(cmd: UpsertDiscountCommandModel): DiscountEntity {
        return discountRepository.saveAndFlush(
            this?.apply {
                amount = cmd.amount
                description = cmd.description
                expiresAt = cmd.expiresAtUtc
            } ?: DiscountEntity(
                amount = cmd.amount,
                code = cmd.code,
                description = cmd.description,
                expiresAt = cmd.expiresAtUtc
            )
        )
    }
}
