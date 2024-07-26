package com.ribbontek.ordermanagement.grpc.model

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.Length

data class RegisterUserCommandModel(
    @get:[
        NotEmpty
        NotBlank
        Email
    ] val email: String,
    @get:[
        NotEmpty
        NotBlank
        Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\^$*.[\\]{}()\\-\"!@#%&/,><':;|_~`]])\\S{8,99}$")
    ] val password: String,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 2, max = 255)
    ] val firstName: String,
    @get:[
        NotEmpty
        NotBlank
        Size(min = 2, max = 255)
    ] val lastName: String
)

data class LoginUserCommandModel(
    @get:[
        NotEmpty
        NotBlank
        Email
    ] val username: String,
    @get:[
        NotEmpty
        NotBlank
    ] val password: String
)

data class ResetPasswordCommandModel(
    @get:[
        NotEmpty
        NotBlank
        Email
    ] val email: String,
    @get:[
        NotEmpty
        NotBlank
        Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\^$*.[\\]{}()\\-\"!@#%&/,><':;|_~`]])\\S{8,99}$")
    ] val password: String,
    @get:[
        NotEmpty
        NotBlank
        Length(min = 6, max = 6)
    ] val code: String
)

data class RefreshTokenCommandModel(
    @get:[
        NotEmpty
        NotBlank
    ] val refreshToken: String
)

data class VerifyMfaCommandModel(
    @get:[
        NotEmpty
        NotBlank
        Length(min = 6, max = 6)
    ] val totpCode: String
)
