package com.ribbontek.ordermanagement.factory

import com.ribbontek.grpccourse.AddItemToShoppingCartCommand
import com.ribbontek.grpccourse.CancelShoppingCartSessionCommand
import com.ribbontek.grpccourse.CreateShoppingCartSessionCommand
import com.ribbontek.grpccourse.RemoveItemFromCartCommand
import com.ribbontek.grpccourse.addItemToShoppingCartCommand
import com.ribbontek.grpccourse.cancelShoppingCartSessionCommand
import com.ribbontek.grpccourse.createShoppingCartSessionCommand
import com.ribbontek.grpccourse.removeItemFromCartCommand
import com.ribbontek.grpccourse.shoppingCartItem
import java.util.UUID

object ShoppingCartSessionCommandFactory {
    fun createSession(): CreateShoppingCartSessionCommand {
        return createShoppingCartSessionCommand {
            id = UUID.randomUUID().toString()
        }
    }

    fun addItemToShoppingCartCommand(
        id: String = UUID.randomUUID().toString(),
        productId: String = UUID.randomUUID().toString(),
        quantity: Int = 1
    ): AddItemToShoppingCartCommand {
        return addItemToShoppingCartCommand {
            this.id = id
            this.cartItem =
                shoppingCartItem {
                    this.productId = productId
                    this.quantity = quantity
                }
        }
    }

    fun removeItemToShoppingCartCommand(
        id: String = UUID.randomUUID().toString(),
        productId: String = UUID.randomUUID().toString(),
        quantity: Int = 1
    ): RemoveItemFromCartCommand {
        return removeItemFromCartCommand {
            this.id = id
            this.cartItem =
                shoppingCartItem {
                    this.productId = productId
                    this.quantity = quantity
                }
        }
    }

    fun cancelSession(id: String = UUID.randomUUID().toString()): CancelShoppingCartSessionCommand {
        return cancelShoppingCartSessionCommand {
            this.id = id
        }
    }
}
