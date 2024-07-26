package com.ribbontek.ordermanagement.service.cart

import com.ribbontek.grpccourse.ShoppingCartItem
import com.ribbontek.grpccourse.ShoppingCartSession
import com.ribbontek.grpccourse.shoppingCartItem
import com.ribbontek.grpccourse.shoppingCartSession
import com.ribbontek.ordermanagement.exception.BadRequestException
import com.ribbontek.ordermanagement.exception.ConflictException
import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.grpc.model.AddItemToShoppingCartCommandModel
import com.ribbontek.ordermanagement.grpc.model.CancelShoppingCartSessionCommandModel
import com.ribbontek.ordermanagement.grpc.model.CreateShoppingCartSessionCommandModel
import com.ribbontek.ordermanagement.grpc.model.RemoveItemFromCartCommandModel
import com.ribbontek.ordermanagement.grpc.model.ShoppingCartItemModel
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartItemEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionRepository
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartStatus
import com.ribbontek.ordermanagement.repository.cart.expectOneBySessionIdAndUserId
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.ordermanagement.repository.product.expectOneByRequestId
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneById
import com.ribbontek.ordermanagement.security.Principal
import com.ribbontek.ordermanagement.util.getPrincipal
import com.ribbontek.ordermanagement.util.toUUID
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

@Validated
interface ShoppingCartService {
    fun createSession(
        @Valid cmd: CreateShoppingCartSessionCommandModel,
        principal: Principal = getPrincipal()
    ): ShoppingCartSession

    fun cancelSession(
        @Valid cmd: CancelShoppingCartSessionCommandModel,
        principal: Principal = getPrincipal()
    ): ShoppingCartSession

    fun addItemToShoppingCart(
        @Valid cmd: AddItemToShoppingCartCommandModel,
        principal: Principal = getPrincipal()
    ): ShoppingCartSession

    fun removeItemFromShoppingCart(
        @Valid cmd: RemoveItemFromCartCommandModel,
        principal: Principal = getPrincipal()
    ): ShoppingCartSession
}

@Service
class ShoppingCartServiceImpl(
    private val shoppingCartSessionRepository: ShoppingCartSessionRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) : ShoppingCartService {
    @Transactional
    override fun createSession(cmd: CreateShoppingCartSessionCommandModel, principal: Principal): ShoppingCartSession {
        if (shoppingCartSessionRepository.existsBySessionIdAndUserId(cmd.id.toUUID(), principal.userId)) {
            throw ConflictException("Shopping cart session already exists with sessionId ${cmd.id} and userId ${principal.userId}")
        }
        return shoppingCartSessionRepository.save(cmd.toShoppingCartEntity(principal)).toShoppingCartSession()
    }

    @Transactional
    override fun cancelSession(cmd: CancelShoppingCartSessionCommandModel, principal: Principal): ShoppingCartSession {
        return shoppingCartSessionRepository.expectOneBySessionIdAndUserId(cmd.id.toUUID(), principal.userId)
            .cancel()
            .toShoppingCartSession()
    }

    @Transactional
    override fun addItemToShoppingCart(cmd: AddItemToShoppingCartCommandModel, principal: Principal): ShoppingCartSession {
        return shoppingCartSessionRepository.expectOneBySessionIdAndUserId(cmd.id.toUUID(), principal.userId)
            .addItem(cmd)
            .toShoppingCartSession()
    }

    @Transactional
    override fun removeItemFromShoppingCart(cmd: RemoveItemFromCartCommandModel, principal: Principal): ShoppingCartSession {
        return shoppingCartSessionRepository.expectOneBySessionIdAndUserId(cmd.id.toUUID(), principal.userId)
            .removeItem(cmd)
            .toShoppingCartSession()
    }

    fun ShoppingCartSessionEntity.removeItem(cmd: RemoveItemFromCartCommandModel): ShoppingCartSessionEntity {
        return shoppingCartSessionRepository.save(
            this.apply {
                val cartItem = cartItems?.find { it.product.requestId == cmd.shoppingCartItem.productId.toUUID() }
                    ?: throw NotFoundException("Cart item not found for product ${cmd.shoppingCartItem.productId}")
                if (cmd.shoppingCartItem.quantity > cartItem.quantity) {
                    throw BadRequestException(
                        "Can't remove more than existing quantity ${cartItem.quantity}"
                    )
                }
                if (cmd.shoppingCartItem.quantity == cartItem.quantity) {
                    cartItems?.remove(cartItem)
                } else {
                    cartItem.quantity -= cmd.shoppingCartItem.quantity
                }

                total = calculateTotal(cartItems?.toList())
            }
        )
    }

    fun ShoppingCartSessionEntity.addItem(cmd: AddItemToShoppingCartCommandModel): ShoppingCartSessionEntity {
        return shoppingCartSessionRepository.save(
            this.apply {
                cartItems?.find { cmd.shoppingCartItem.productId.toUUID() == it.product.requestId }?.let {
                    it.quantity += cmd.shoppingCartItem.quantity
                } ?: run {
                    cartItems?.add(cmd.shoppingCartItem.toShoppingCartItemEntity(this)) ?: run {
                        cartItems = mutableSetOf(cmd.shoppingCartItem.toShoppingCartItemEntity(this))
                    }
                }
                status = ShoppingCartStatus.MODIFIED.name
                total = calculateTotal(cartItems?.toList())
            }
        )
    }

    private fun calculateTotal(cartItems: List<ShoppingCartItemEntity>?): BigDecimal {
        return cartItems?.run {
            val productIds = mapNotNull { item -> item.product.id }.distinct()
            when {
                productIds.isNotEmpty() -> {
                    val productsMap = productRepository.findEagerByIdIn(productIds).associateBy { it.id }
                    sumOf { cartItem ->
                        val foundProduct =
                            productsMap[cartItem.product.id]
                                ?: throw BadRequestException("Could not find product id ${cartItem.product.id} for cart item")
                        val total = foundProduct.price.setScale(2, HALF_UP).multiply(cartItem.quantity.toBigDecimal())
                        foundProduct.discount?.amount?.divide(BigDecimal(100))?.setScale(2, HALF_UP)?.let {
                            total.minus(total.multiply(it)).setScale(2, HALF_UP)
                        } ?: total.setScale(2, HALF_UP)
                    }
                }
                else -> BigDecimal.ZERO
            }
        } ?: BigDecimal.ZERO
    }

    private fun ShoppingCartItemModel.toShoppingCartItemEntity(session: ShoppingCartSessionEntity): ShoppingCartItemEntity {
        val item = this
        return ShoppingCartItemEntity(
            session = session,
            quantity = item.quantity,
            product = productRepository.expectOneByRequestId(item.productId.toUUID())
        )
    }

    private fun ShoppingCartSessionEntity.cancel(): ShoppingCartSessionEntity {
        return shoppingCartSessionRepository.save(
            this.apply {
                status = ShoppingCartStatus.CANCELLED.name
            }
        )
    }

    private fun CreateShoppingCartSessionCommandModel.toShoppingCartEntity(principal: Principal): ShoppingCartSessionEntity {
        return ShoppingCartSessionEntity(
            sessionId = id.toUUID(),
            status = ShoppingCartStatus.STARTED.name,
            user = userRepository.expectOneById(principal.userId),
            total = BigDecimal.ZERO
        )
    }

    private fun ShoppingCartSessionEntity.toShoppingCartSession(): ShoppingCartSession {
        val entity = this
        return shoppingCartSession {
            this.sessionId = entity.sessionId.toString()
            this.status = entity.status
            this.total = entity.total.toFloat()
            entity.cartItems?.map { it.toShoppingCartItem() }?.let { this.cartItems.addAll(it) }
        }
    }

    private fun ShoppingCartItemEntity.toShoppingCartItem(): ShoppingCartItem {
        val entity = this
        return shoppingCartItem {
            this.productId = entity.product.requestId.toString()
            this.quantity = entity.quantity
        }
    }
}
