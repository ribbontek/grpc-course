package com.ribbontek.ordermanagement.grpc

import com.ribbontek.grpccourse.AddItemToShoppingCartCommand
import com.ribbontek.grpccourse.CancelShoppingCartSessionCommand
import com.ribbontek.grpccourse.CreateShoppingCartSessionCommand
import com.ribbontek.grpccourse.RemoveItemFromCartCommand
import com.ribbontek.grpccourse.ShoppingCartServiceGrpcKt.ShoppingCartServiceCoroutineImplBase
import com.ribbontek.grpccourse.ShoppingCartSession
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toAddItemToShoppingCartCommandModel
import com.ribbontek.ordermanagement.mapping.toCancelShoppingCartSessionCommandModel
import com.ribbontek.ordermanagement.mapping.toCreateShoppingCartSessionCommandModel
import com.ribbontek.ordermanagement.mapping.toRemoveItemFromCartCommandModel
import com.ribbontek.ordermanagement.security.RequiresOAuthPermission
import com.ribbontek.ordermanagement.service.cart.ShoppingCartService

@RibbontekGrpcService
class ShoppingCartGrpcService(
    private val shoppingCartService: ShoppingCartService
) : ShoppingCartServiceCoroutineImplBase() {

    @RequiresOAuthPermission("shopping:create")
    override suspend fun createSession(request: CreateShoppingCartSessionCommand): ShoppingCartSession {
        return shoppingCartService.createSession(request.toCreateShoppingCartSessionCommandModel())
    }

    @RequiresOAuthPermission("shopping:delete")
    override suspend fun cancelSession(request: CancelShoppingCartSessionCommand): ShoppingCartSession {
        return shoppingCartService.cancelSession(request.toCancelShoppingCartSessionCommandModel())
    }

    @RequiresOAuthPermission("shopping:edit")
    override suspend fun addItemToShoppingCart(request: AddItemToShoppingCartCommand): ShoppingCartSession {
        return shoppingCartService.addItemToShoppingCart(request.toAddItemToShoppingCartCommandModel())
    }

    @RequiresOAuthPermission("shopping:edit")
    override suspend fun removeItemFromShoppingCart(request: RemoveItemFromCartCommand): ShoppingCartSession {
        return shoppingCartService.removeItemFromShoppingCart(request.toRemoveItemFromCartCommandModel())
    }
}
