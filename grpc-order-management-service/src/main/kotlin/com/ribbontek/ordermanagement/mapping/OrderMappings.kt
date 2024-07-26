package com.ribbontek.ordermanagement.mapping

import com.ribbontek.grpccourse.AddAddressesCommand
import com.ribbontek.grpccourse.CompleteOrderCommand
import com.ribbontek.grpccourse.OrderAddress
import com.ribbontek.grpccourse.OrderAddressType.BILLING
import com.ribbontek.grpccourse.OrderAddressType.DELIVERY
import com.ribbontek.grpccourse.Payment
import com.ribbontek.grpccourse.ShoppingCartToOrderCommand
import com.ribbontek.ordermanagement.exception.BadRequestException
import com.ribbontek.ordermanagement.grpc.model.AddAddressesCommandModel
import com.ribbontek.ordermanagement.grpc.model.CompleteOrderCommandModel
import com.ribbontek.ordermanagement.grpc.model.OrderAddressModel
import com.ribbontek.ordermanagement.grpc.model.PaymentModel
import com.ribbontek.ordermanagement.grpc.model.ShoppingCartToOrderCommandModel
import com.ribbontek.ordermanagement.repository.user.AddressTypeEnum

// todo convert to a string & use a pattern validation annotation, then this can map safely to the entity
fun ShoppingCartToOrderCommand.toShoppingCartToOrderCommandModel(): ShoppingCartToOrderCommandModel {
    return ShoppingCartToOrderCommandModel(
        sessionId = this.id
    )
}

fun AddAddressesCommand.toAddAddressesCommandModel(): AddAddressesCommandModel {
    return AddAddressesCommandModel(
        sessionId = this.id,
        orderAddressModels = this.addressList.map { it.toOrderAddressModel() }
    )
}

private fun OrderAddress.toOrderAddressModel(): OrderAddressModel {
    return OrderAddressModel(
        addressType =
        when (this.addressType) {
            DELIVERY -> AddressTypeEnum.DELIVERY
            BILLING -> AddressTypeEnum.BILLING
            else -> throw BadRequestException("Unsupported address type ${this.addressType}")
        },
        line = this.line,
        suburb = this.suburb,
        state = if (this.hasState()) this.state else null,
        postcode = if (this.hasPostcode()) this.postcode else null,
        country = this.country
    )
}

fun CompleteOrderCommand.toCompleteOrderCommandModel(): CompleteOrderCommandModel {
    return CompleteOrderCommandModel(
        sessionId = this.id,
        paymentModel = this.payment.toPaymentModel()
    )
}

private fun Payment.toPaymentModel(): PaymentModel {
    return PaymentModel(
        amount = this.amount.toBigDecimal(),
        provider = this.provider,
        reference = this.reference
    )
}
