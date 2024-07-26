package com.ribbontek.ordermanagement.generator

import com.ribbontek.ordermanagement.factory.entity.CategoryEntityFactory
import com.ribbontek.ordermanagement.factory.entity.DiscountEntityFactory
import com.ribbontek.ordermanagement.factory.entity.ProductEntityFactory
import com.ribbontek.ordermanagement.repository.product.CategoryEnum
import com.ribbontek.ordermanagement.repository.product.CategoryRepository
import com.ribbontek.ordermanagement.repository.product.DiscountRepository
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.ordermanagement.repository.product.expectOneByCode
import com.ribbontek.ordermanagement.security.TestSecurityContext
import com.ribbontek.ordermanagement.util.FakerUtil
import com.ribbontek.shared.util.mapAsync
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

interface ProductGenerator {
    fun generateProducts(count: Int = 10): List<ProductEntity>
}

@Component
class ProductGeneratorImpl(
    private val productRepository: ProductRepository,
    private val discountRepository: DiscountRepository,
    private val categoryRepository: CategoryRepository
) : ProductGenerator {
    @Transactional
    override fun generateProducts(count: Int): List<ProductEntity> {
        TestSecurityContext.configureMockUser()
        return runBlocking {
            (1..count).mapAsync {
                ProductEntityFactory.create(
                    discount = when {
                        it % 3 == 0 -> discountRepository.save(DiscountEntityFactory.create())
                        else -> null
                    },
                    category = when {
                        it % 2 == 0 -> categoryRepository.save(CategoryEntityFactory.create())
                        else -> categoryRepository.expectOneByCode(FakerUtil.enum<CategoryEnum>().name)
                    }
                )
            }.run { productRepository.saveAll(this) }
        }
    }
}
