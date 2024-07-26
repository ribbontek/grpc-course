@file:Suppress("ktlint:filename")

package com.ribbontek.ordermanagement.grpc.model

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class UnsubscribeUserCommandModel(
    @get:[
        NotEmpty
        NotBlank
        Email
    ]
    val email: String,
    @get:[
        NotEmpty
        NotBlank
        Size(max = 8)
    ]
    val code: String
)
