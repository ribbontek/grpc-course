package com.ribbontek.ordermanagement.grpc

import com.ribbontek.grpccourse.AddAddressesCommand
import com.ribbontek.grpccourse.CompleteOrderCommand
import com.ribbontek.grpccourse.Order
import com.ribbontek.grpccourse.OrderServiceGrpcKt.OrderServiceCoroutineImplBase
import com.ribbontek.grpccourse.ShoppingCartToOrderCommand
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toAddAddressesCommandModel
import com.ribbontek.ordermanagement.mapping.toCompleteOrderCommandModel
import com.ribbontek.ordermanagement.mapping.toShoppingCartToOrderCommandModel
import com.ribbontek.ordermanagement.security.RequiresOAuthPermission
import com.ribbontek.ordermanagement.service.order.OrderService

@RibbontekGrpcService
class OrderGrpcService(
    private val orderService: OrderService
) : OrderServiceCoroutineImplBase() {

    @RequiresOAuthPermission("order:create")
    override suspend fun convertShoppingCartToOrder(request: ShoppingCartToOrderCommand): Order {
        return orderService.convertShoppingCartToOrder(request.toShoppingCartToOrderCommandModel())
    }

    @RequiresOAuthPermission("order:edit")
    override suspend fun addAddresses(request: AddAddressesCommand): Order {
        return orderService.addAddresses(request.toAddAddressesCommandModel())
    }

    @RequiresOAuthPermission("order:edit")
    override suspend fun completeOrder(request: CompleteOrderCommand): Order {
        return orderService.completeOrder(request.toCompleteOrderCommandModel())
    }
}
