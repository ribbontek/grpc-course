package com.ribbontek.ordermanagement.grpc

import com.ribbontek.grpccourse.admin.AdminCategory
import com.ribbontek.grpccourse.admin.AdminDiscount
import com.ribbontek.grpccourse.admin.AdminProduct
import com.ribbontek.grpccourse.admin.AdminProductServiceGrpcKt.AdminProductServiceCoroutineStub
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.AdminPagingRequestFactory
import com.ribbontek.ordermanagement.factory.UpsertCategoryCommandFactory
import com.ribbontek.ordermanagement.factory.UpsertDiscountCommandFactory
import com.ribbontek.ordermanagement.factory.UpsertProductCommandFactory
import com.ribbontek.ordermanagement.repository.product.CategoryRepository
import com.ribbontek.ordermanagement.repository.product.DiscountRepository
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.shared.util.toUtc
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.math.ceil

class AdminProductGrpcServiceTest : AbstractIntegTest() {
    @GrpcClient("clientstub")
    private lateinit var adminProductServiceCoroutineStub: AdminProductServiceCoroutineStub

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var discountRepository: DiscountRepository

    @AfterAll
    fun afterAll() {
        productRepository.deleteAll()
    }

    @Test
    fun `upsert category - insert & update successfully - admin user`() {
        withAdminUser {
            // create
            val category = UpsertCategoryCommandFactory.create()
            runBlocking {
                adminProductServiceCoroutineStub.upsertCategory(category, authMetadata)
            }
            val categoryEntity = categoryRepository.findByCode(category.code) ?: fail("Could not find category entity")
            assertThat(category.code, equalTo(categoryEntity.code))
            assertThat(category.description, equalTo(categoryEntity.description))
            assertNotNull(categoryEntity.createdAt)
            assertNull(categoryEntity.modifiedAt)
            assertFalse(categoryEntity.deleted)

            // update
            val categoryUpdate = UpsertCategoryCommandFactory.create(code = category.code)
            runBlocking {
                adminProductServiceCoroutineStub.upsertCategory(categoryUpdate, authMetadata)
            }
            val categoryEntityUpdated = categoryRepository.findByCode(categoryUpdate.code) ?: fail("Could not find category entity")
            assertThat(categoryUpdate.code, equalTo(categoryEntityUpdated.code))
            assertThat(categoryUpdate.description, equalTo(categoryEntityUpdated.description))
            assertNotNull(categoryEntityUpdated.createdAt)
            assertNotNull(categoryEntityUpdated.modifiedAt)
            assertFalse(categoryEntityUpdated.deleted)
        }
    }

    @Test
    fun `upsert category - insert & update auth fail - standard user`() {
        withStandardUser {
            val category = UpsertCategoryCommandFactory.create()
            val result = assertThrows<StatusException> {
                runBlocking { adminProductServiceCoroutineStub.upsertCategory(category, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
            assertNull(categoryRepository.findByCode(category.code))
        }
    }

    @Test
    fun `upsert category - insert & update auth fail - no user`() {
        val category = UpsertCategoryCommandFactory.create()
        val result = assertThrows<StatusException> {
            runBlocking { adminProductServiceCoroutineStub.upsertCategory(category) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        assertNull(categoryRepository.findByCode(category.code))
    }

    @Test
    fun `get paged categories - no filter - admin user`() {
        withAdminUser {
            // set up test data
            repeat(100) {
                runBlocking {
                    adminProductServiceCoroutineStub.upsertCategory(UpsertCategoryCommandFactory.create(), authMetadata)
                }
            }
            val categoryPageRequest = AdminPagingRequestFactory.adminCategories()
            val result = runBlocking {
                adminProductServiceCoroutineStub.getAdminPagedCategories(categoryPageRequest, authMetadata)
            }
            val totalCategories = categoryRepository.findAll().associateBy { it.code!! }
            assertThat(result.totalPages.toDouble(), equalTo(ceil(totalCategories.size / 100.0)))
            assertThat(result.size, equalTo(100))
            assertThat(result.number, equalTo(0))
            assertThat(result.numberOfElements, equalTo(100))
            assertThat(result.totalElements, equalTo(totalCategories.size.toLong()))
            assertFalse(result.sorted)
            assertTrue(result.contentList.isNotEmpty())
            val content = result.contentList.map { it.unpack(AdminCategory::class.java) }
            content.forEach {
                val categoryEntity = totalCategories[it.code] ?: fail("Could not find category")
                assertThat(it.code, equalTo(categoryEntity.code))
                assertThat(it.description, equalTo(categoryEntity.description))
                assertNotNull(it.createdAtUtc)
                assertFalse(it.hasModifiedAtUtc())
                assertFalse(categoryEntity.deleted)
            }
        }
    }

    @Test
    fun `get paged categories - auth fail - standard user`() {
        withStandardUser {
            val categoryPageRequest = AdminPagingRequestFactory.adminCategories()
            val result = assertThrows<StatusException> {
                runBlocking { adminProductServiceCoroutineStub.getAdminPagedCategories(categoryPageRequest, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
        }
    }

    @Test
    fun `get paged categories - auth fail - no user`() {
        val categoryPageRequest = AdminPagingRequestFactory.adminCategories()
        val result = assertThrows<StatusException> {
            runBlocking { adminProductServiceCoroutineStub.getAdminPagedCategories(categoryPageRequest) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `upsert discount - insert & update successfully - admin user`() {
        withAdminUser {
            // create
            val discount = UpsertDiscountCommandFactory.create()
            runBlocking {
                adminProductServiceCoroutineStub.upsertDiscount(discount, authMetadata)
            }
            val categoryEntity = discountRepository.findByCode(discount.code) ?: fail("Could not find discount entity")
            assertThat(discount.code, equalTo(categoryEntity.code))
            assertThat(discount.description, equalTo(categoryEntity.description))
            assertThat(discount.amount, equalTo(categoryEntity.amount.toFloat()))
            assertThat(
                ZonedDateTime.parse(discount.expiresAtUtc).toUtc().toEpochSecond(),
                equalTo(categoryEntity.expiresAt!!.toUtc().toEpochSecond())
            )
            assertNotNull(categoryEntity.createdAt)
            assertNotNull(categoryEntity.createdBy)
            assertNull(categoryEntity.modifiedAt)
            assertNull(categoryEntity.modifiedBy)
            assertFalse(categoryEntity.deleted)

            // update
            val discountUpdate = UpsertDiscountCommandFactory.create(discount.code)
            runBlocking {
                adminProductServiceCoroutineStub.upsertDiscount(discountUpdate, authMetadata)
            }
            val categoryEntityUpdated = discountRepository.findByCode(discountUpdate.code) ?: fail("Could not find discount entity")
            assertThat(discountUpdate.code, equalTo(categoryEntityUpdated.code))
            assertThat(discountUpdate.description, equalTo(categoryEntityUpdated.description))
            assertThat(discountUpdate.amount, equalTo(categoryEntityUpdated.amount.toFloat()))
            assertThat(
                ZonedDateTime.parse(discountUpdate.expiresAtUtc).toUtc().toEpochSecond(),
                equalTo(categoryEntityUpdated.expiresAt!!.toUtc().toEpochSecond())
            )
            assertNotNull(categoryEntityUpdated.createdAt)
            assertNotNull(categoryEntityUpdated.createdBy)
            assertNotNull(categoryEntityUpdated.modifiedAt)
            assertNotNull(categoryEntityUpdated.modifiedBy)
            assertFalse(categoryEntityUpdated.deleted)
        }
    }

    @Test
    fun `upsert discount - insert & update auth fail - standard user`() {
        withStandardUser {
            val discount = UpsertDiscountCommandFactory.create()
            val result = assertThrows<StatusException> {
                runBlocking { adminProductServiceCoroutineStub.upsertDiscount(discount) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
            assertNull(discountRepository.findByCode(discount.code))
        }
    }

    @Test
    fun `upsert discount - insert & update auth fail - no user`() {
        val discount = UpsertDiscountCommandFactory.create()
        val result = assertThrows<StatusException> {
            runBlocking { adminProductServiceCoroutineStub.upsertDiscount(discount) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        assertNull(discountRepository.findByCode(discount.code))
    }

    @Test
    fun `get paged discounts - no filter - admin user`() {
        withAdminUser {
            // set up test data
            repeat(100) {
                runBlocking {
                    adminProductServiceCoroutineStub.upsertDiscount(UpsertDiscountCommandFactory.create(), authMetadata)
                }
            }
            val discountsPageRequest = AdminPagingRequestFactory.adminDiscounts()
            val result = runBlocking {
                adminProductServiceCoroutineStub.getAdminPagedDiscounts(discountsPageRequest, authMetadata)
            }
            val totalDiscounts = discountRepository.findAll().associateBy { it.code }
            assertThat(result.totalPages.toDouble(), equalTo(ceil(totalDiscounts.size / 100.0)))
            assertThat(result.size, equalTo(100))
            assertThat(result.number, equalTo(0))
            assertThat(result.numberOfElements, equalTo(100))
            assertThat(result.totalElements, equalTo(totalDiscounts.size.toLong()))
            assertFalse(result.sorted)
            assertTrue(result.contentList.isNotEmpty())
            val content = result.contentList.map { it.unpack(AdminDiscount::class.java) }
            content.forEach {
                val discountEntity = totalDiscounts[it.code] ?: fail("Could not find discount")
                assertThat(it.code, equalTo(discountEntity.code))
                assertThat(it.expiresAtUtc, equalTo(discountEntity.expiresAt.toString()))
                assertThat(
                    it.amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                    equalTo(discountEntity.amount.setScale(2, RoundingMode.HALF_UP))
                )
                assertNotNull(it.createdAtUtc)
                assertNotNull(it.createdBy)
                assertFalse(it.hasModifiedAtUtc())
                assertFalse(it.hasModifiedBy())
                assertFalse(discountEntity.deleted)
            }
        }
    }

    @Test
    fun `get paged discounts - auth fail - standard user`() {
        withStandardUser {
            val discountsPageRequest = AdminPagingRequestFactory.adminDiscounts()
            val result = assertThrows<StatusException> {
                runBlocking { adminProductServiceCoroutineStub.getAdminPagedDiscounts(discountsPageRequest, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
        }
    }

    @Test
    fun `get paged discounts - auth fail - no user`() {
        val discountsPageRequest = AdminPagingRequestFactory.adminDiscounts()
        val result = assertThrows<StatusException> {
            runBlocking { adminProductServiceCoroutineStub.getAdminPagedDiscounts(discountsPageRequest) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `upsert product - insert & update successfully - admin user`() {
        withAdminUser {
            // create
            val product = UpsertProductCommandFactory.create()
            runBlocking {
                adminProductServiceCoroutineStub.upsertProduct(product, authMetadata)
            }
            val productEntity = productRepository.findByRequestId(UUID.fromString(product.requestId))
                ?: fail("Could not find product entity")
            assertThat(product.requestId, equalTo(productEntity.requestId.toString()))
            assertThat(product.title, equalTo(productEntity.title))
            assertThat(product.description, equalTo(productEntity.description))
            assertThat(product.quantity, equalTo(productEntity.quantity))
            assertThat(product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP), equalTo(productEntity.price))
            assertThat(product.sku, equalTo(productEntity.sku))
            assertThat(product.categoryCode, equalTo(productEntity.category.code))
            productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
            assertNotNull(productEntity.createdAt)
            assertNotNull(productEntity.createdBy)
            assertNull(productEntity.modifiedAt)
            assertNull(productEntity.modifiedBy)
            assertFalse(productEntity.deleted)

            // update
            val productUpdate = UpsertProductCommandFactory.create(product.requestId)
            runBlocking {
                adminProductServiceCoroutineStub.upsertProduct(productUpdate, authMetadata)
            }
            val categoryEntityUpdated = productRepository.findByRequestId(UUID.fromString(product.requestId))
                ?: fail("Could not find product entity")
            assertThat(productUpdate.requestId, equalTo(categoryEntityUpdated.requestId.toString()))
            assertThat(productUpdate.title, equalTo(categoryEntityUpdated.title))
            assertThat(productUpdate.description, equalTo(categoryEntityUpdated.description))
            assertThat(productUpdate.quantity, equalTo(categoryEntityUpdated.quantity))
            assertThat(product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP), equalTo(productEntity.price))
            assertThat(productUpdate.sku, equalTo(categoryEntityUpdated.sku))
            assertThat(productUpdate.categoryCode, equalTo(categoryEntityUpdated.category.code))
            productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
            assertNotNull(categoryEntityUpdated.createdAt)
            assertNotNull(categoryEntityUpdated.createdBy)
            assertNotNull(categoryEntityUpdated.modifiedAt)
            assertNotNull(categoryEntityUpdated.modifiedBy)
            assertFalse(categoryEntityUpdated.deleted)
        }
    }

    @Test
    fun `upsert product - insert & update auth fail - standard user`() {
        withStandardUser {
            val product = UpsertProductCommandFactory.create()
            val result = assertThrows<StatusException> {
                runBlocking { adminProductServiceCoroutineStub.upsertProduct(product) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
            assertNull(productRepository.findByRequestId(UUID.fromString(product.requestId)))
        }
    }

    @Test
    fun `upsert product - insert & update auth fail - no user`() {
        val category = UpsertProductCommandFactory.create()
        val result = assertThrows<StatusException> {
            runBlocking { adminProductServiceCoroutineStub.upsertProduct(category) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        assertNull(productRepository.findByRequestId(UUID.fromString(category.requestId)))
    }

    @Test
    fun `get paged products - no filter - admin user`() {
        withAdminUser {
            // set up test data
            repeat(100) {
                runBlocking {
                    adminProductServiceCoroutineStub.upsertProduct(UpsertProductCommandFactory.create(), authMetadata)
                }
            }
            val productsPageRequest = AdminPagingRequestFactory.adminProducts()
            val result = runBlocking {
                adminProductServiceCoroutineStub.getAdminPagedProducts(productsPageRequest, authMetadata)
            }
            val totalProducts = productRepository.findAllIncludingDeleted().associateBy { it.requestId }
            assertThat(result.totalPages.toDouble(), equalTo(ceil(totalProducts.size / 100.0)))
            assertThat(result.size, equalTo(100))
            assertThat(result.number, equalTo(0))
            assertThat(result.numberOfElements, equalTo(100))
            assertThat(result.totalElements, equalTo(totalProducts.size.toLong()))
            assertFalse(result.sorted)
            assertTrue(result.contentList.isNotEmpty())
            val content = result.contentList.map { it.unpack(AdminProduct::class.java) }
            content.forEach { product ->
                val productEntity = totalProducts[UUID.fromString(product.requestId)] ?: fail("Could not find product")
                assertThat(product.requestId, equalTo(productEntity.requestId.toString()))
                assertThat(product.title, equalTo(productEntity.title))
                assertThat(product.description, equalTo(productEntity.description))
                assertThat(product.quantity, equalTo(productEntity.quantity))
                assertThat(product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP), equalTo(productEntity.price))
                assertThat(product.sku, equalTo(productEntity.sku))
                assertThat(product.categoryCode, equalTo(productEntity.category.code))
                productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
                assertThat(product.createdAtUtc, equalTo(productEntity.createdAt!!.toUtc().toString()))
                assertThat(product.createdBy, equalTo(productEntity.createdBy))
                if (product.hasModifiedAtUtc()) assertThat(product.modifiedAtUtc, equalTo(productEntity.modifiedAt!!.toUtc().toString()))
                if (product.hasModifiedBy()) assertThat(product.modifiedBy, equalTo(productEntity.modifiedBy))
            }
        }
    }

    @Test
    fun `get paged products - auth fail - standard user`() {
        withStandardUser {
            val productsPageRequest = AdminPagingRequestFactory.adminProducts()
            val result = assertThrows<StatusException> {
                runBlocking { adminProductServiceCoroutineStub.getAdminPagedProducts(productsPageRequest, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
            assertThat(result.status.description, equalTo("Invalid Authentication"))
        }
    }

    @Test
    fun `get paged products - auth fail - no user`() {
        val productsPageRequest = AdminPagingRequestFactory.adminProducts()
        val result = assertThrows<StatusException> {
            runBlocking { adminProductServiceCoroutineStub.getAdminPagedProducts(productsPageRequest) }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }
}
