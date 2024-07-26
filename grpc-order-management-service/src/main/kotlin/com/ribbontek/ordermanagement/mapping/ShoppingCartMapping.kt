package com.ribbontek.ordermanagement.mapping

import com.ribbontek.grpccourse.AddItemToShoppingCartCommand
import com.ribbontek.grpccourse.CancelShoppingCartSessionCommand
import com.ribbontek.grpccourse.CreateShoppingCartSessionCommand
import com.ribbontek.grpccourse.RemoveItemFromCartCommand
import com.ribbontek.ordermanagement.grpc.model.AddItemToShoppingCartCommandModel
import com.ribbontek.ordermanagement.grpc.model.CancelShoppingCartSessionCommandModel
import com.ribbontek.ordermanagement.grpc.model.CreateShoppingCartSessionCommandModel
import com.ribbontek.ordermanagement.grpc.model.RemoveItemFromCartCommandModel
import com.ribbontek.ordermanagement.grpc.model.ShoppingCartItemModel

fun CreateShoppingCartSessionCommand.toCreateShoppingCartSessionCommandModel(): CreateShoppingCartSessionCommandModel {
    return CreateShoppingCartSessionCommandModel(id = this.id)
}

fun CancelShoppingCartSessionCommand.toCancelShoppingCartSessionCommandModel(): CancelShoppingCartSessionCommandModel {
    return CancelShoppingCartSessionCommandModel(id = this.id)
}

fun AddItemToShoppingCartCommand.toAddItemToShoppingCartCommandModel(): AddItemToShoppingCartCommandModel {
    return AddItemToShoppingCartCommandModel(
        id = this.id,
        shoppingCartItem =
        ShoppingCartItemModel(
            productId = cartItem.productId,
            quantity = cartItem.quantity
        )
    )
}

fun RemoveItemFromCartCommand.toRemoveItemFromCartCommandModel(): RemoveItemFromCartCommandModel {
    return RemoveItemFromCartCommandModel(
        id = this.id,
        shoppingCartItem =
        ShoppingCartItemModel(
            productId = cartItem.productId,
            quantity = cartItem.quantity
        )
    )
}
