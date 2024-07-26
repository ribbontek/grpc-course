package com.ribbontek.ordermanagement.grpc

import com.ribbontek.grpccourse.ShoppingCartServiceGrpcKt.ShoppingCartServiceCoroutineStub
import com.ribbontek.grpccourse.copy
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.factory.ShoppingCartSessionCommandFactory
import com.ribbontek.ordermanagement.generator.ProductGenerator
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionRepository
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartStatus
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.ordermanagement.util.toUUID
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.util.UUID

class ShoppingCartGrpcServiceTest : AbstractIntegTest() {
    @GrpcClient("clientstub")
    private lateinit var shoppingCartServiceCoroutineStub: ShoppingCartServiceCoroutineStub

    @Autowired
    private lateinit var productGenerator: ProductGenerator

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var shoppingCartSessionRepository: ShoppingCartSessionRepository
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
    fun `create shopping cart session - standard user - success`() {
        withStandardUser {
            val sessionCmd = ShoppingCartSessionCommandFactory.createSession()
            val session = runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) }
            val sessionEntity = shoppingCartSessionRepository.findBySessionIdAndUserId(
                sessionId = UUID.fromString(session.sessionId),
                userId = userId
            )
            assertNotNull(sessionEntity)
            assertThat(session.sessionId, equalTo(sessionEntity!!.sessionId.toString()))
            assertThat(session.status, equalTo(sessionEntity.status))
            assertThat(session.total.toBigDecimal().setScale(2), equalTo(sessionEntity.total))
            assertTrue(session.cartItemsList.isEmpty())
        }
    }

    @Test
    fun `create shopping cart session - standard user - conflict exception`() {
        withStandardUser {
            val sessionCmd = ShoppingCartSessionCommandFactory.createSession()
            runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) }
            val result = assertThrows<StatusException> {
                runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.ALREADY_EXISTS.code))
            assertThat(
                result.status.description,
                equalTo("Shopping cart session already exists with sessionId ${sessionCmd.id} and userId $userId")
            )
        }
    }

    @Test
    fun `create shopping cart session - standard user - bad request exception`() {
        withStandardUser {
            val sessionCmd = ShoppingCartSessionCommandFactory.createSession().copy { this.id = "asdf" }
            val result = assertThrows<StatusException> { runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) } }
            assertThat(result.status.code, equalTo(Status.INVALID_ARGUMENT.code))
            assertThat(
                result.status.description,
                equalTo("[id]: must match \"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$\"")
            )
        }
    }

    @Test
    fun `create shopping cart session - no user - auth exception`() {
        val sessionCmd = ShoppingCartSessionCommandFactory.createSession()
        val result = assertThrows<StatusException> { runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd) } }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
        assertFalse(shoppingCartSessionRepository.existsBySessionId(UUID.fromString(sessionCmd.id)))
    }

    @Test
    fun `add item to shopping cart session - standard user - success`() {
        withStandardUser {
            val sessionCmd = ShoppingCartSessionCommandFactory.createSession()
            runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) }
            val product = productEntities.random()
            val addItemCmd = ShoppingCartSessionCommandFactory.addItemToShoppingCartCommand(
                id = sessionCmd.id,
                productId = product.requestId.toString()
            )
            val session = runBlocking { shoppingCartServiceCoroutineStub.addItemToShoppingCart(addItemCmd, authMetadata) }
            val sessionEntity = shoppingCartSessionRepository.findBySessionIdAndUserId(
                sessionId = UUID.fromString(session.sessionId),
                userId = userId
            )
            assertNotNull(sessionEntity)
            assertThat(session.sessionId, equalTo(sessionEntity!!.sessionId.toString()))
            assertThat(session.status, equalTo(sessionEntity.status))
            assertThat(session.status, equalTo(ShoppingCartStatus.MODIFIED.name))
            assertThat(session.total.toBigDecimal().setScale(2, HALF_UP), equalTo(sessionEntity.total.setScale(2, HALF_UP)))
            assertThat(session.total.toBigDecimal().setScale(2, HALF_UP), equalTo(product.getPrice().setScale(2, HALF_UP)))
            assertTrue(session.cartItemsList.isNotEmpty())
            session.cartItemsList.forEach { cartItem ->
                val cartItemEntity = sessionEntity.cartItems!!.first { it.product.requestId.toString() == cartItem.productId }
                assertThat(cartItem.quantity, equalTo(cartItemEntity.quantity))
            }
        }
    }

    @Test
    fun `add item to shopping cart session - no user - auth exception`() {
        val addItemCmd = ShoppingCartSessionCommandFactory.addItemToShoppingCartCommand()
        val result = assertThrows<StatusException> { runBlocking { shoppingCartServiceCoroutineStub.addItemToShoppingCart(addItemCmd) } }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `remove item from shopping cart session - standard user - success`() {
        withStandardUser {
            val sessionCmd = ShoppingCartSessionCommandFactory.createSession()
            runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) }
            val product1 = productEntities.random()
            val product2 = productEntities.random()
            val addItemCmd1 = ShoppingCartSessionCommandFactory.addItemToShoppingCartCommand(
                id = sessionCmd.id,
                productId = product1.requestId.toString()
            )
            val addItemCmd2 = ShoppingCartSessionCommandFactory.addItemToShoppingCartCommand(
                id = sessionCmd.id,
                productId = product2.requestId.toString()
            )
            val addItemCmd1v2 = ShoppingCartSessionCommandFactory.addItemToShoppingCartCommand(
                id = sessionCmd.id,
                productId = product1.requestId.toString(),
                quantity = 2
            )
            // add item for product 1
            val session1 = runBlocking { shoppingCartServiceCoroutineStub.addItemToShoppingCart(addItemCmd1, authMetadata) }
            assertThat(session1.total.toBigDecimal().setScale(2), equalTo(product1.getPrice()))
            // add same item again for product 1 with 2 more
            val session2 = runBlocking { shoppingCartServiceCoroutineStub.addItemToShoppingCart(addItemCmd1v2, authMetadata) }
            assertThat(session2.total.toBigDecimal().setScale(2), equalTo(product1.getPrice(3)))
            // add item for product 2
            val session3 = runBlocking { shoppingCartServiceCoroutineStub.addItemToShoppingCart(addItemCmd2, authMetadata) }
            assertThat(
                session3.total.toBigDecimal().setScale(2, HALF_UP),
                equalTo((product1.getPrice(3).plus(product2.getPrice())).setScale(2, HALF_UP))
            )
            // remove item for product 2 - quantity exceeds amount added to cart
            val removeItemCmdBadRequest = ShoppingCartSessionCommandFactory.removeItemToShoppingCartCommand(
                id = sessionCmd.id,
                productId = product2.requestId.toString(),
                quantity = 10
            )
            val result = assertThrows<StatusException> {
                runBlocking { shoppingCartServiceCoroutineStub.removeItemFromShoppingCart(removeItemCmdBadRequest, authMetadata) }
            }
            assertThat(result.status.code, equalTo(Status.FAILED_PRECONDITION.code))
            assertThat(result.status.description, equalTo("Can't remove more than existing quantity 1"))
            // remove item for product 2 - bad product id
            val product3 = (productEntities - product1 - product2).random()
            val removeItemCmdBadRequest2 =
                ShoppingCartSessionCommandFactory.removeItemToShoppingCartCommand(
                    id = sessionCmd.id,
                    productId = product3.requestId.toString(),
                    quantity = 10
                )
            val result2 = assertThrows<StatusException> {
                runBlocking { shoppingCartServiceCoroutineStub.removeItemFromShoppingCart(removeItemCmdBadRequest2, authMetadata) }
            }
            assertThat(result2.status.code, equalTo(Status.NOT_FOUND.code))
            assertThat(result2.status.description, equalTo("Cart item not found for product ${product3.requestId}"))
            // remove item for product 2 - valid
            val removeItemCmd = ShoppingCartSessionCommandFactory.removeItemToShoppingCartCommand(
                id = sessionCmd.id,
                productId = product2.requestId.toString()
            )
            val session = runBlocking { shoppingCartServiceCoroutineStub.removeItemFromShoppingCart(removeItemCmd, authMetadata) }
            val sessionEntity = shoppingCartSessionRepository.findBySessionIdAndUserId(
                sessionId = UUID.fromString(session.sessionId),
                userId = userId
            )
            assertNotNull(sessionEntity)
            assertThat(session.sessionId, equalTo(sessionEntity!!.sessionId.toString()))
            assertThat(session.status, equalTo(sessionEntity.status))
            assertThat(session.total.toBigDecimal().setScale(2, HALF_UP), equalTo(sessionEntity.total.setScale(2, HALF_UP)))
            assertThat(session.total.toBigDecimal().setScale(2, HALF_UP), equalTo((product1.getPrice(3)).setScale(2, HALF_UP)))
            assertTrue(session.cartItemsList.isNotEmpty())
            session.cartItemsList.forEach { cartItem ->
                val cartItemEntity = sessionEntity.cartItems!!.first { it.product.requestId.toString() == cartItem.productId }
                assertThat(cartItem.quantity, equalTo(cartItemEntity.quantity))
            }
        }
    }

    @Test
    fun `remove item from shopping cart session - no user - auth exception`() {
        val removeItemCmd = ShoppingCartSessionCommandFactory.removeItemToShoppingCartCommand()
        val result = assertThrows<StatusException> { runBlocking { shoppingCartServiceCoroutineStub.removeItemFromShoppingCart(removeItemCmd) } }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    @Test
    fun `cancel shopping cart session - standard user - success`() {
        withStandardUser {
            val sessionCmd = ShoppingCartSessionCommandFactory.createSession()
            runBlocking { shoppingCartServiceCoroutineStub.createSession(sessionCmd, authMetadata) }
            val cancelCmd = ShoppingCartSessionCommandFactory.cancelSession(id = sessionCmd.id)
            val session = runBlocking { shoppingCartServiceCoroutineStub.cancelSession(cancelCmd, authMetadata) }
            val sessionEntity = shoppingCartSessionRepository.findBySessionIdAndUserId(
                sessionId = session.sessionId.toUUID(),
                userId = userId
            )
            assertNull(sessionEntity)
            assertTrue(shoppingCartSessionRepository.existsBySessionIdAndUserId(cancelCmd.id.toUUID(), userId))
        }
    }

    @Test
    fun `cancel shopping cart session - standard user - not found exception`() {
        withStandardUser {
            val cancelCmd = ShoppingCartSessionCommandFactory.cancelSession()
            val result = assertThrows<StatusException> { runBlocking { shoppingCartServiceCoroutineStub.cancelSession(cancelCmd, authMetadata) } }
            assertThat(result.status.code, equalTo(Status.NOT_FOUND.code))
            assertThat(
                result.status.description,
                equalTo("Could not find shopping cart session with sessionId ${cancelCmd.id} and userId $userId")
            )
        }
    }

    @Test
    fun `cancel shopping cart session - no user - auth exception`() {
        val cancelCmd = ShoppingCartSessionCommandFactory.cancelSession()
        val result = assertThrows<StatusException> { runBlocking { shoppingCartServiceCoroutineStub.cancelSession(cancelCmd) } }
        assertThat(result.status.code, equalTo(Status.PERMISSION_DENIED.code))
        assertThat(result.status.description, equalTo("Invalid Authentication"))
    }

    private fun ProductEntity.getPrice(quantity: Int = 1): BigDecimal {
        val total = price.setScale(2, HALF_UP).multiply(quantity.toBigDecimal())
        return discount?.amount?.divide(BigDecimal(100))?.setScale(2, HALF_UP)?.let {
            total.minus(total.multiply(it)).setScale(2, HALF_UP)
        } ?: total.setScale(2, HALF_UP)
    }
}
