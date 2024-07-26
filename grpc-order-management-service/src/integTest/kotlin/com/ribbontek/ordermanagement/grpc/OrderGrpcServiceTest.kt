package com.ribbontek.ordermanagement.grpc

import com.ribbontek.grpccourse.Order
import com.ribbontek.grpccourse.OrderServiceGrpcKt.OrderServiceCoroutineStub
import com.ribbontek.grpccourse.ShoppingCartToOrderCommand
import com.ribbontek.grpccourse.shoppingCartToOrderCommand
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.OrderCommandFactory
import com.ribbontek.ordermanagement.generator.ProductGenerator
import com.ribbontek.ordermanagement.generator.ShoppingCartGenerator
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartStatus.CONVERTED
import com.ribbontek.ordermanagement.repository.order.OrderRepository
import com.ribbontek.ordermanagement.repository.order.OrderStatus.CONFIRMED
import com.ribbontek.ordermanagement.repository.order.OrderStatus.MODIFIED
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.ordermanagement.repository.user.AddressTypeEnum
import com.ribbontek.ordermanagement.util.FakerUtil
import com.ribbontek.ordermanagement.util.toUUID
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.client.inject.GrpcClient
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.RoundingMode.HALF_UP
import java.util.UUID

class OrderGrpcServiceTest : AbstractIntegTest() {
    @GrpcClient("clientstub")
    private lateinit var orderServiceCoroutineStub: OrderServiceCoroutineStub

    @Autowired
    private lateinit var productGenerator: ProductGenerator

    @Autowired
    private lateinit var shoppingCartGenerator: ShoppingCartGenerator

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository
    private lateinit var productEntities: List<ProductEntity>

    @BeforeAll
    fun beforeAll() {
        productEntities = productGenerator.generateProducts(10)
    }

    @AfterAll
    fun afterAll() {
        productRepository.deleteAll(productEntities)
    }

    @Test
    fun `create order from shopping cart session - standard user - success`() {
        withStandardUser {
            val newOrder = generateOrder(userId, authMetadata)
            val order = orderRepository.findEagerBySessionIdAndUserId(newOrder.sessionId.toUUID(), userId)
            assertNotNull(order)
            assertThat(newOrder.sessionId, equalTo(order!!.sessionId.toString()))
            assertThat(newOrder.total.toBigDecimal().setScale(2, HALF_UP), equalTo(order.total))
            assertThat(newOrder.status, equalTo(CONVERTED.name)) // indicate the status is CONVERTED explicitly
            assertThat(newOrder.status, equalTo(order.status))
            assertThat(newOrder.orderItemsCount, equalTo(order.orderItems?.size ?: 0))
            assertThat(newOrder.orderAddressesCount, equalTo(0)) // expect it to be zero at this stage
            assertThat(newOrder.orderAddressesCount, equalTo(order.addresses?.size ?: 0))
            assertTrue(newOrder.orderItemsList.isNotEmpty())
            newOrder.orderItemsList.forEach { orderItem ->
                val itemEntity = order.orderItems?.first { orderItem.productId == it.product!!.requestId.toString() }
                assertNotNull(itemEntity)
                assertThat(orderItem.quantity, equalTo(itemEntity!!.quantity))
            }
            assertTrue(newOrder.orderAddressesList.isEmpty())
            assertFalse(newOrder.hasPayment())
        }
    }

    @Test
    fun `add addresses to order - standard user - success`() {
        withStandardUser {
            val generated = generateOrder(userId, authMetadata)
            val newOrder = runBlocking {
                orderServiceCoroutineStub.addAddresses(OrderCommandFactory.addAddresses(id = generated.sessionId), authMetadata)
            }
            val order = orderRepository.findEagerBySessionIdAndUserId(newOrder.sessionId.toUUID(), userId)
            assertNotNull(order)
            assertThat(newOrder.sessionId, equalTo(order!!.sessionId.toString()))
            assertThat(newOrder.total.toBigDecimal().setScale(2, HALF_UP), equalTo(order.total))
            assertThat(newOrder.status, equalTo(MODIFIED.name)) // indicate the status is MODIFIED explicitly
            assertThat(newOrder.status, equalTo(order.status))
            assertThat(newOrder.orderItemsCount, equalTo(order.orderItems?.size ?: 0))
            assertThat(newOrder.orderAddressesCount, equalTo(order.addresses?.size ?: 0))
            newOrder.orderItemsList.forEach { orderItem ->
                val itemEntity = order.orderItems?.first { orderItem.productId == it.product!!.requestId.toString() }
                assertNotNull(itemEntity)
                assertThat(orderItem.quantity, equalTo(itemEntity!!.quantity))
            }
            newOrder.orderAddressesList.forEach { orderAddress ->
                val addressEntity = order.addresses?.toList()?.first { (addressType, _) ->
                    AddressTypeEnum.findByCode(addressType.code!!)?.name == orderAddress.addressType.name
                }?.second
                assertNotNull(addressEntity)
                assertThat(orderAddress.line, equalTo(addressEntity!!.line))
                assertThat(orderAddress.suburb, equalTo(addressEntity.suburb))
                assertThat(orderAddress.country, equalTo(addressEntity.country))
                if (orderAddress.hasState()) assertThat(orderAddress.state, equalTo(addressEntity.state))
                if (orderAddress.hasPostcode()) assertThat(orderAddress.postcode, equalTo(addressEntity.postcode))
            }
            assertFalse(newOrder.hasPayment()) // not completed order yet
        }
    }

    @Test
    fun `complete order - standard user - success`() {
        withStandardUser {
            val generated = generateOrder(userId, authMetadata)
            val newOrder = runBlocking {
                orderServiceCoroutineStub.completeOrder(OrderCommandFactory.completeOrder(id = generated.sessionId), authMetadata)
            }
            val order = orderRepository.findEagerBySessionIdAndUserId(newOrder.sessionId.toUUID(), userId)
            assertNotNull(order)
            assertThat(newOrder.sessionId, equalTo(order!!.sessionId.toString()))
            assertThat(newOrder.total.toBigDecimal().setScale(2, HALF_UP), equalTo(order.total))
            assertThat(newOrder.status, equalTo(CONFIRMED.name)) // indicate the status is CONFIRMED explicitly
            assertThat(newOrder.status, equalTo(order.status))
            assertThat(newOrder.orderItemsCount, equalTo(order.orderItems?.size ?: 0))
            assertThat(newOrder.orderAddressesCount, equalTo(order.addresses?.size ?: 0))
            newOrder.orderItemsList.forEach { orderItem ->
                val itemEntity = order.orderItems?.first { orderItem.productId == it.product!!.requestId.toString() }
                assertNotNull(itemEntity)
                assertThat(orderItem.quantity, equalTo(itemEntity!!.quantity))
            }
            assertTrue(newOrder.hasPayment()) // not completed order yet
            assertThat(newOrder.payment.reference, equalTo(order.payment?.reference))
            assertThat(newOrder.payment.amount.toBigDecimal().setScale(2, HALF_UP), equalTo(order.payment?.amount))
            assertThat(newOrder.payment.provider, equalTo(order.payment?.provider))
        }
    }

    @Test
    fun `create shopping cart session - no user - auth exception`() {
        val sessionCmd = OrderCommandFactory.shoppingCartToOrder()
        val result = assertThrows<StatusException> {
            runBlocking {
                orderServiceCoroutineStub.convertShoppingCartToOrder(sessionCmd)
            }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        assertFalse(orderRepository.existsBySessionId(sessionCmd.id.toUUID()))
    }

    @Test
    fun `add addresses to order - no user - auth exception`() {
        val result = assertThrows<StatusException> {
            runBlocking {
                orderServiceCoroutineStub.addAddresses(OrderCommandFactory.addAddresses(id = UUID.randomUUID().toString()))
            }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `complete order - no user - auth exception`() {
        val result = assertThrows<StatusException> {
            runBlocking {
                orderServiceCoroutineStub.completeOrder(OrderCommandFactory.completeOrder(id = UUID.randomUUID().toString()))
            }
        }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    private fun ShoppingCartSessionEntity.toShoppingCartToOrderCommand(): ShoppingCartToOrderCommand =
        shoppingCartToOrderCommand { id = sessionId.toString() }

    private fun generateOrder(userId: Long, authMetadata: Metadata): Order {
        val product1 = productEntities.random()
        val product2 = (productEntities - product1).random()
        val shoppingCartSession = shoppingCartGenerator.generateShoppingCartForUserEntity(
            userId = userId,
            quantityToProductList = listOf(
                FakerUtil.quantity() to product1,
                FakerUtil.quantity() to product2
            )
        )
        return runBlocking {
            orderServiceCoroutineStub.convertShoppingCartToOrder(shoppingCartSession.toShoppingCartToOrderCommand(), authMetadata)
        }
    }
}
