package com.ribbontek.ordermanagement.generator

import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.factory.entity.ShoppingCartItemEntityFactory
import com.ribbontek.ordermanagement.factory.entity.ShoppingCartSessionEntityFactory
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionRepository
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

interface ShoppingCartGenerator {
    fun generateShoppingCartForUserEntity(userId: Long, quantityToProductList: List<Pair<Int, ProductEntity>>): ShoppingCartSessionEntity
}

@Component
class ShoppingCartGeneratorImpl(
    private val shoppingCartSessionRepository: ShoppingCartSessionRepository,
    private val userRepository: UserRepository
) : ShoppingCartGenerator {
    @Transactional
    override fun generateShoppingCartForUserEntity(
        userId: Long,
        quantityToProductList: List<Pair<Int, ProductEntity>>
    ): ShoppingCartSessionEntity {
        val shoppingCart = shoppingCartSessionRepository.save(
            ShoppingCartSessionEntityFactory.create(
                userEntity = userRepository.findByIdOrNull(userId) ?: throw NotFoundException("Could not find user with id $userId")
            )
        )
        return shoppingCartSessionRepository.save(
            shoppingCart.apply {
                cartItems = quantityToProductList.map {
                    ShoppingCartItemEntityFactory.create(session = shoppingCart, quantity = it.first, productEntity = it.second)
                }.toMutableSet()
                total = cartItems?.sumOf { (it.quantity.toBigDecimal() * it.product.price) } ?: BigDecimal.ZERO
            }
        )
    }
}
