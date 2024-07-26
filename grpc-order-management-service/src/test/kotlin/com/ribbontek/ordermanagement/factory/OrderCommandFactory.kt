package com.ribbontek.ordermanagement.factory

import com.ribbontek.grpccourse.AddAddressesCommand
import com.ribbontek.grpccourse.CompleteOrderCommand
import com.ribbontek.grpccourse.OrderAddressType
import com.ribbontek.grpccourse.ShoppingCartToOrderCommand
import com.ribbontek.grpccourse.addAddressesCommand
import com.ribbontek.grpccourse.completeOrderCommand
import com.ribbontek.grpccourse.orderAddress
import com.ribbontek.grpccourse.payment
import com.ribbontek.grpccourse.shoppingCartToOrderCommand
import com.ribbontek.ordermanagement.util.FakerUtil
import java.util.UUID

object OrderCommandFactory {
    fun shoppingCartToOrder(id: String = UUID.randomUUID().toString()): ShoppingCartToOrderCommand {
        return shoppingCartToOrderCommand {
            this.id = id
        }
    }

    fun addAddresses(id: String = UUID.randomUUID().toString()): AddAddressesCommand {
        return addAddressesCommand {
            this.id = id
            this.address.addAll(
                listOf(
                    orderAddress {
                        addressType = OrderAddressType.DELIVERY
                        line = FakerUtil.addressLine()
                        suburb = FakerUtil.suburb()
                        state = FakerUtil.state()
                        postcode = FakerUtil.postcode()
                        country = FakerUtil.country()
                    },
                    orderAddress {
                        addressType = OrderAddressType.BILLING
                        line = FakerUtil.addressLine()
                        suburb = FakerUtil.suburb()
                        state = FakerUtil.state()
                        postcode = FakerUtil.postcode()
                        country = FakerUtil.country()
                    }
                )
            )
        }
    }

    fun completeOrder(id: String = UUID.randomUUID().toString()): CompleteOrderCommand {
        return completeOrderCommand {
            this.id = id
            payment =
                payment {
                    amount = FakerUtil.price().toFloat()
                    provider = FakerUtil.alphanumeric(50)
                    reference = FakerUtil.alphanumeric(50)
                }
        }
    }
}
