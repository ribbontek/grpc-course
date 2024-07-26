package com.ribbontek.ordermanagement.service.order

import com.ribbontek.grpccourse.Order
import com.ribbontek.grpccourse.OrderAddress
import com.ribbontek.grpccourse.OrderAddressType
import com.ribbontek.grpccourse.OrderItem
import com.ribbontek.grpccourse.Payment
import com.ribbontek.grpccourse.order
import com.ribbontek.grpccourse.orderAddress
import com.ribbontek.grpccourse.orderItem
import com.ribbontek.grpccourse.payment
import com.ribbontek.ordermanagement.exception.BadRequestException
import com.ribbontek.ordermanagement.exception.ConflictException
import com.ribbontek.ordermanagement.grpc.model.AddAddressesCommandModel
import com.ribbontek.ordermanagement.grpc.model.CompleteOrderCommandModel
import com.ribbontek.ordermanagement.grpc.model.ShoppingCartToOrderCommandModel
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartItemEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionEntity
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionRepository
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartStatus
import com.ribbontek.ordermanagement.repository.cart.expectOneBySessionIdAndUserId
import com.ribbontek.ordermanagement.repository.order.OrderAddressEntity
import com.ribbontek.ordermanagement.repository.order.OrderEntity
import com.ribbontek.ordermanagement.repository.order.OrderItemEntity
import com.ribbontek.ordermanagement.repository.order.OrderRepository
import com.ribbontek.ordermanagement.repository.order.OrderStatus
import com.ribbontek.ordermanagement.repository.order.PaymentEntity
import com.ribbontek.ordermanagement.repository.order.expectOneBySessionIdAndUserId
import com.ribbontek.ordermanagement.repository.product.ProductEntity
import com.ribbontek.ordermanagement.repository.product.ProductRepository
import com.ribbontek.ordermanagement.repository.user.AddressTypeEntity
import com.ribbontek.ordermanagement.repository.user.AddressTypeEnum.BILLING
import com.ribbontek.ordermanagement.repository.user.AddressTypeEnum.DELIVERY
import com.ribbontek.ordermanagement.repository.user.AddressTypeRepository
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneByCode
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
interface OrderService {
    fun convertShoppingCartToOrder(
        @Valid cmd: ShoppingCartToOrderCommandModel,
        principal: Principal = getPrincipal()
    ): Order

    fun addAddresses(
        @Valid cmd: AddAddressesCommandModel,
        principal: Principal = getPrincipal()
    ): Order

    fun completeOrder(
        @Valid cmd: CompleteOrderCommandModel,
        principal: Principal = getPrincipal()
    ): Order
}

@Service
class OrderServiceImpl(
    private val shoppingCartSessionRepository: ShoppingCartSessionRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val addressTypeRepository: AddressTypeRepository
) : OrderService {
    @Transactional
    override fun convertShoppingCartToOrder(cmd: ShoppingCartToOrderCommandModel, principal: Principal): Order {
        if (orderRepository.existsBySessionIdAndUserId(cmd.sessionId.toUUID(), principal.userId)) {
            throw ConflictException("Order already exists with sessionId ${cmd.sessionId} and userId ${principal.userId}")
        }
        return shoppingCartSessionRepository.expectOneBySessionIdAndUserId(
            cmd.sessionId.toUUID(),
            principal.userId
        )
            .convertToOrder(principal.userId)
            .toOrder()
    }

    @Transactional
    override fun addAddresses(cmd: AddAddressesCommandModel, principal: Principal): Order {
        return orderRepository.expectOneBySessionIdAndUserId(cmd.sessionId.toUUID(), principal.userId)
            .addAddresses(cmd)
            .toOrder()
    }

    @Transactional
    override fun completeOrder(cmd: CompleteOrderCommandModel, principal: Principal): Order {
        return orderRepository.expectOneBySessionIdAndUserId(cmd.sessionId.toUUID(), principal.userId)
            .completeOrder(cmd, principal.userId)
            .toOrder()
    }

    private fun ShoppingCartSessionEntity.convertToOrder(userId: Long): OrderEntity {
        if (cartItems.isNullOrEmpty()) throw BadRequestException("Cannot create an order with no items")
        val products = cartItems?.let { item -> productRepository.findEagerByIdIn(item.mapNotNull { it.product.id }.distinct()) } ?: emptyList()
        val order = orderRepository.save(
            OrderEntity(
                sessionId = sessionId,
                user = userRepository.expectOneById(userId),
                status = ShoppingCartStatus.CONVERTED.name,
                total = total
            )
        )
        return orderRepository.save(
            order.apply {
                orderItems = cartItems?.map { it.toOrderItemEntity(order, products) }?.toMutableSet()
            }
        )
    }

    private fun OrderEntity.addAddresses(cmd: AddAddressesCommandModel): OrderEntity {
        return orderRepository.save(
            this.apply {
                addresses = cmd.orderAddressModels.associate {
                    val addressType = when (it.addressType) {
                        DELIVERY, BILLING -> addressTypeRepository.expectOneByCode(it.addressType.code)
                        else -> throw BadRequestException("Invalid address type found for order ${it.addressType}")
                    }
                    addressType to OrderAddressEntity(
                        order = this,
                        line = it.line,
                        suburb = it.suburb,
                        state = it.state,
                        postcode = it.postcode,
                        country = it.country,
                        addressType = addressType
                    )
                }.toMutableMap()
                status = OrderStatus.MODIFIED.name
            }
        )
    }

    private fun OrderEntity.completeOrder(
        cmd: CompleteOrderCommandModel,
        userId: Long
    ): OrderEntity {
        return orderRepository.save(
            this.apply {
                if (status == OrderStatus.CONFIRMED.name) throw BadRequestException("Already confirmed order with id ${cmd.sessionId}")
                status = OrderStatus.CONFIRMED.name
                payment = PaymentEntity(
                    user = userRepository.expectOneById(userId),
                    amount = cmd.paymentModel.amount,
                    provider = cmd.paymentModel.provider,
                    reference = cmd.paymentModel.reference
                )
            }
        )
    }

    private fun OrderEntity.toOrder(): Order {
        val source = this
        return order {
            this.sessionId = source.sessionId.toString()
            this.total = source.total.setScale(2, HALF_UP).toFloat()
            this.status = source.status
            this.orderItems.addAll(source.orderItems?.map { it.toOrderItem() } ?: emptyList())
            this.orderAddresses.addAll(source.addresses?.map { it.toPair().toOrderAddress() } ?: emptyList())
            source.payment?.let { this.payment = it.toPayment() }
        }
    }

    private fun Pair<AddressTypeEntity, OrderAddressEntity>.toOrderAddress(): OrderAddress {
        val (addressTypeEntity, orderAddressEntity) = this
        return orderAddress {
            this.addressType = when (addressTypeEntity.code) {
                DELIVERY.code -> OrderAddressType.DELIVERY
                BILLING.code -> OrderAddressType.BILLING
                else -> throw BadRequestException("Incorrect mapping for address types")
            }
            this.line = orderAddressEntity.line
            this.suburb = orderAddressEntity.suburb
            orderAddressEntity.state?.let { this.state = it }
            orderAddressEntity.postcode?.let { this.postcode = it }
            this.country = orderAddressEntity.country
        }
    }

    private fun PaymentEntity.toPayment(): Payment {
        val source = this
        return payment {
            this.reference = source.reference
            this.amount = source.amount.toFloat()
            this.provider = source.provider
        }
    }

    private fun OrderItemEntity.toOrderItem(): OrderItem {
        val source = this
        return orderItem {
            productId = source.product!!.requestId.toString()
            quantity = source.quantity
        }
    }

    private fun ShoppingCartItemEntity.toOrderItemEntity(orderEntity: OrderEntity, products: List<ProductEntity>): OrderItemEntity {
        return OrderItemEntity(
            order = orderEntity,
            product = this.product,
            quantity = this.quantity,
            price = this.let { cartItem ->
                val foundProduct = products.first { it.id == cartItem.product.id }
                foundProduct.discount?.amount?.divide(BigDecimal(100))?.let {
                    foundProduct.price.minus(foundProduct.price.multiply(it))
                } ?: foundProduct.price
            }
        )
    }
}
