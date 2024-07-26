package com.ribbontek.ordermanagement.grpc.model

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ShoppingCartItemModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val productId: String,
    @get:[
        Size(min = 1, max = 1000)
    ] val quantity: Int
)

data class CreateShoppingCartSessionCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val id: String
)

data class CancelShoppingCartSessionCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val id: String
)

data class AddItemToShoppingCartCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val id: String,
    val shoppingCartItem: ShoppingCartItemModel
)

data class RemoveItemFromCartCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val id: String,
    val shoppingCartItem: ShoppingCartItemModel
)
