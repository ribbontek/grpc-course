package com.ribbontek.ordermanagement.grpc.model

import com.ribbontek.ordermanagement.grpc.model.validator.NotBlankOrEmpty
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.ZonedDateTime

data class UpsertDiscountCommandModel(
    @get:[
        Positive
        DecimalMax("100.0")
    ] val amount: BigDecimal,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 1, max = 50)
    ] val code: String,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 1, max = 255)
    ] val description: String,
    val expiresAtUtc: ZonedDateTime
)

data class UpsertCategoryCommandModel(
    @get:[
        NotEmpty
        NotBlank
        Size(min = 1, max = 50)
    ] val code: String,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 1, max = 1000)
    ] val description: String
)

data class UpsertProductCommandModel(
    @get:[
        Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")
    ]
    val requestId: String,
    @get:[
        NotBlankOrEmpty
    ] val discountCode: String?,
    @get:[
        NotEmpty
        NotBlank
    ] val categoryCode: String,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 1, max = 255)
    ] val title: String,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 1, max = 1000)
    ] val description: String,
    @get:[Positive] val price: BigDecimal,
    @get:[Positive] val quantity: Long,
    @get:[
        NotBlankOrEmpty
        Size(min = 0, max = 255)
    ] val sku: String?
)
