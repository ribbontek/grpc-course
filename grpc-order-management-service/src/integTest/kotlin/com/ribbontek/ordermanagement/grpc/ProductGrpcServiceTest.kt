package com.ribbontek.ordermanagement.grpc

import com.google.protobuf.Empty
import com.ribbontek.grpccourse.Direction.ASC
import com.ribbontek.grpccourse.Direction.DESC
import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.PagingResponse
import com.ribbontek.grpccourse.Product
import com.ribbontek.grpccourse.ProductServiceGrpcKt.ProductServiceCoroutineStub
import com.ribbontek.grpccourse.pagingRequest
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.ProductPagingRequestFactory
import com.ribbontek.ordermanagement.generator.ProductGenerator
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import java.math.RoundingMode
import java.util.UUID

class ProductGrpcServiceTest : AbstractIntegTest() {
    @GrpcClient("clientstub")
    private lateinit var productServiceCoroutineStub: ProductServiceCoroutineStub

    @Autowired
    private lateinit var productGenerator: ProductGenerator

    @Autowired
    private lateinit var productRepository: ProductRepository

    private lateinit var productEntities: List<ProductEntity>

    @BeforeAll
    fun beforeAll() {
        productEntities = productGenerator.generateProducts(100)
    }

    @AfterAll
    fun afterAll() {
        productRepository.deleteAll(productEntities)
    }

    @Test
    fun `get all products`() {
        val productMutableList = mutableListOf<Product>()
        runBlocking { productServiceCoroutineStub.getProducts(Empty.getDefaultInstance()).toList(productMutableList) }
        assertTrue(productMutableList.isNotEmpty())
        assertThat(productMutableList.size, greaterThanOrEqualTo(productEntities.size))

        val productMap = productMutableList.associateBy { UUID.fromString(it.requestId) }
        productEntities.forEach { productEntity ->
            val product = productMap[productEntity.requestId] ?: fail("Could not find product")
            assertThat(product.requestId, equalTo(productEntity.requestId.toString()))
            assertThat(product.title, equalTo(productEntity.title))
            assertThat(product.description, equalTo(productEntity.description))
            assertThat(product.quantity, equalTo(productEntity.quantity))
            assertThat(
                product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                equalTo(productEntity.price.setScale(2, RoundingMode.HALF_UP))
            )
            assertThat(product.sku, equalTo(productEntity.sku))
            assertThat(product.categoryCode, equalTo(productEntity.category.code))
            productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
            assertFalse(productEntity.deleted)
        }
    }

    @Test
    fun `get all products - no filter - paged`() {
        val products = getAllPaged(ProductPagingRequestFactory.products(size = 8))

        assertTrue(products.isNotEmpty())
        assertThat(products.size, greaterThanOrEqualTo(productEntities.size))

        val productMap = products.associateBy { UUID.fromString(it.requestId) }
        productEntities.forEach { productEntity ->
            val product = productMap[productEntity.requestId] ?: fail("Could not find product")
            assertThat(product.requestId, equalTo(productEntity.requestId.toString()))
            assertThat(product.title, equalTo(productEntity.title))
            assertThat(product.description, equalTo(productEntity.description))
            assertThat(product.quantity, equalTo(productEntity.quantity))
            assertThat(
                product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                equalTo(productEntity.price.setScale(2, RoundingMode.HALF_UP))
            )
            assertThat(product.sku, equalTo(productEntity.sku))
            assertThat(product.categoryCode, equalTo(productEntity.category.code))
            productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
            assertFalse(productEntity.deleted)
        }
    }

    @Test
    fun `get all products - filter - paged`() {
        val productsSearch = productRepository.findAll().groupBy { it.category.code }.maxByOrNull { (_, value) -> value.size }
        assertNotNull(productsSearch)

        val products = getAllPaged(
            ProductPagingRequestFactory.products(
                categoryCodeLookup = productsSearch!!.key,
                sorted = ProductPagingRequestFactory.sorted(direction = ASC)
            )
        )

        assertTrue(products.isNotEmpty())
        assertThat(products.size, equalTo(productsSearch.value.size))

        val productMap = products.associateBy { UUID.fromString(it.requestId) }
        productsSearch.value.sortedBy { it.createdAt }.forEach { productEntity ->
            val product = productMap[productEntity.requestId] ?: fail("Could not find product")
            assertThat(product.requestId, equalTo(productEntity.requestId.toString()))
            assertThat(product.title, equalTo(productEntity.title))
            assertThat(product.description, equalTo(productEntity.description))
            assertThat(product.quantity, equalTo(productEntity.quantity))
            assertThat(
                product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                equalTo(productEntity.price.setScale(2, RoundingMode.HALF_UP))
            )
            assertThat(product.sku, equalTo(productEntity.sku))
            assertThat(product.categoryCode, equalTo(productEntity.category.code))
            productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
            assertFalse(productEntity.deleted)
        }

        val productsSearch2 = productRepository.findAll().filter { it.title.startsWith("TEST-") }

        val products2 = getAllPaged(
            ProductPagingRequestFactory.products(
                searchFor = "TEST-",
                sorted = ProductPagingRequestFactory.sorted(direction = DESC)
            )
        )

        assertTrue(products2.isNotEmpty())
        assertThat(products2.size, equalTo(productsSearch2.size))

        val productMap2 = products2.associateBy { UUID.fromString(it.requestId) }
        productsSearch2.sortedBy { it.createdAt }.reversed().forEach { productEntity ->
            val product = productMap2[productEntity.requestId] ?: fail("Could not find product")
            assertThat(product.requestId, equalTo(productEntity.requestId.toString()))
            assertThat(product.title, equalTo(productEntity.title))
            assertThat(product.description, equalTo(productEntity.description))
            assertThat(product.quantity, equalTo(productEntity.quantity))
            assertThat(
                product.price.toBigDecimal().setScale(2, RoundingMode.HALF_UP),
                equalTo(productEntity.price.setScale(2, RoundingMode.HALF_UP))
            )
            assertThat(product.sku, equalTo(productEntity.sku))
            assertThat(product.categoryCode, equalTo(productEntity.category.code))
            productEntity.discount?.code?.let { assertThat(product.discountCode, equalTo(it)) }
            assertFalse(productEntity.deleted)
        }
    }

    private fun getAllPaged(pagingRequest: PagingRequest): List<Product> {
        val products = mutableListOf<Product>()
        var response: PagingResponse
        var pagingRequestMutable = pagingRequest
        do {
            response = runBlocking {
                productServiceCoroutineStub.getPagedProducts(pagingRequestMutable)
            }
            products.addAll(response.contentList.map { it.unpack(Product::class.java) })
            pagingRequestMutable = pagingRequestMutable.next()
        } while (response.number < response.totalPages)
        return products.toList()
    }

    private fun PagingRequest.next(): PagingRequest =
        let { source ->
            pagingRequest {
                this.number = source.number + 1
                this.size = source.size
                if (source.hasSorted()) this.sorted = source.sorted
                if (source.hasQuery()) this.query = source.query
            }
        }
}
