package com.ribbontek.ordermanagement.mapping

import com.ribbontek.grpccourse.LoginUserCommand
import com.ribbontek.grpccourse.RefreshTokenCommand
import com.ribbontek.grpccourse.RegisterUserCommand
import com.ribbontek.grpccourse.ResetPasswordCommand
import com.ribbontek.grpccourse.VerifyMfaCommand
import com.ribbontek.ordermanagement.grpc.model.LoginUserCommandModel
import com.ribbontek.ordermanagement.grpc.model.RefreshTokenCommandModel
import com.ribbontek.ordermanagement.grpc.model.RegisterUserCommandModel
import com.ribbontek.ordermanagement.grpc.model.ResetPasswordCommandModel
import com.ribbontek.ordermanagement.grpc.model.VerifyMfaCommandModel
import com.ribbontek.ordermanagement.repository.user.UserEntity

fun RegisterUserCommandModel.toUserEntity(): UserEntity {
    return UserEntity(
        email = email,
        firstName = firstName,
        lastName = lastName
    )
}

fun RegisterUserCommand.toRegisterUserCommandModel(): RegisterUserCommandModel {
    return RegisterUserCommandModel(
        email = email,
        password = password,
        firstName = firstName,
        lastName = lastName
    )
}

fun LoginUserCommand.toLoginUserCommandModel(): LoginUserCommandModel {
    return LoginUserCommandModel(
        username = username,
        password = password
    )
}

fun ResetPasswordCommand.toResetPasswordCommandModel(): ResetPasswordCommandModel {
    return ResetPasswordCommandModel(
        email = email,
        password = password,
        code = code
    )
}

fun RefreshTokenCommand.toRefreshTokenCommandModel(): RefreshTokenCommandModel {
    return RefreshTokenCommandModel(
        refreshToken = refreshToken
    )
}

fun VerifyMfaCommand.toVerifyMfaCommandModel(): VerifyMfaCommandModel {
    return VerifyMfaCommandModel(
        totpCode = totpCode
    )
}
