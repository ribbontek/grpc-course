package com.ribbontek.ordermanagement.grpc.model

import com.ribbontek.ordermanagement.repository.user.AddressTypeEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class ShoppingCartToOrderCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val sessionId: String
)

data class OrderAddressModel(
    val addressType: AddressTypeEnum,
    @get:[
        NotEmpty
        NotBlank
    ] val line: String,
    @get:[
        NotEmpty
        NotBlank
    ] val suburb: String,
    @get:[
        NotEmpty
        NotBlank
    ] val state: String?,
    @get:[
        NotEmpty
        NotBlank
    ] val postcode: String?,
    @get:[
        NotEmpty
        NotBlank
    ] val country: String
)

data class AddAddressesCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val sessionId: String,
    @get:[
        Size(min = 2, max = 2)
    ] val orderAddressModels: List<OrderAddressModel>
)

data class PaymentModel(
    val amount: BigDecimal,
    @get:[
        NotEmpty
        NotBlank
    ] val provider: String,
    @get:[
        NotEmpty
        NotBlank
    ] val reference: String
)

data class CompleteOrderCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val sessionId: String,
    val paymentModel: PaymentModel
)
