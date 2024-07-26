package com.ribbontek.ordermanagement.grpc

import com.google.protobuf.Empty
import com.ribbontek.grpccourse.AuthenticationServiceGrpcKt.AuthenticationServiceCoroutineImplBase
import com.ribbontek.grpccourse.LoginResponse
import com.ribbontek.grpccourse.LoginUserCommand
import com.ribbontek.grpccourse.RefreshTokenCommand
import com.ribbontek.grpccourse.RegisterUserCommand
import com.ribbontek.grpccourse.ResetPasswordCommand
import com.ribbontek.grpccourse.TurnOnMfaResponse
import com.ribbontek.grpccourse.VerifyMfaCommand
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toLoginUserCommandModel
import com.ribbontek.ordermanagement.mapping.toRefreshTokenCommandModel
import com.ribbontek.ordermanagement.mapping.toRegisterUserCommandModel
import com.ribbontek.ordermanagement.mapping.toResetPasswordCommandModel
import com.ribbontek.ordermanagement.mapping.toVerifyMfaCommandModel
import com.ribbontek.ordermanagement.security.RequiresOAuthPermission
import com.ribbontek.ordermanagement.service.authentication.LoginUserService
import com.ribbontek.ordermanagement.service.authentication.MfaService
import com.ribbontek.ordermanagement.service.authentication.RefreshTokenService
import com.ribbontek.ordermanagement.service.authentication.RegisterUserService
import com.ribbontek.ordermanagement.service.authentication.ResetPasswordService
import com.ribbontek.ordermanagement.util.getPrincipal

@RibbontekGrpcService
class AuthenticationGrpcService(
    private val loginUserService: LoginUserService,
    private val registerUserService: RegisterUserService,
    private val resetPasswordService: ResetPasswordService,
    private val refreshTokenService: RefreshTokenService,
    private val mfaService: MfaService
) : AuthenticationServiceCoroutineImplBase() {

    override suspend fun login(request: LoginUserCommand): LoginResponse {
        return loginUserService.loginUser(request.toLoginUserCommandModel())
    }

    override suspend fun register(request: RegisterUserCommand): Empty {
        registerUserService.registerUser(request.toRegisterUserCommandModel())
        return Empty.getDefaultInstance()
    }

    @RequiresOAuthPermission("account:resetpassword")
    override suspend fun resetPassword(request: Empty): Empty {
        resetPasswordService.resetPassword(getPrincipal())
        return Empty.getDefaultInstance()
    }

    @RequiresOAuthPermission("account:resetpassword")
    override suspend fun confirmResetPassword(request: ResetPasswordCommand): Empty {
        resetPasswordService.confirmResetPassword(request.toResetPasswordCommandModel())
        return Empty.getDefaultInstance()
    }

    @RequiresOAuthPermission("account:refresh")
    override suspend fun refreshToken(request: RefreshTokenCommand): LoginResponse {
        return refreshTokenService.refreshToken(request.toRefreshTokenCommandModel())
    }

    @RequiresOAuthPermission("account:setupmfa")
    override suspend fun turnOnMFA(request: Empty): TurnOnMfaResponse {
        return mfaService.turnOnMFA()
    }

    @RequiresOAuthPermission("account:verifymfa")
    override suspend fun verifyMFA(request: VerifyMfaCommand): Empty {
        mfaService.verifyMFA(request.toVerifyMfaCommandModel())
        return Empty.getDefaultInstance()
    }

    @RequiresOAuthPermission("account:turnoffmfa")
    override suspend fun turnOffMFA(request: Empty): Empty {
        mfaService.turnOffMFA()
        return Empty.getDefaultInstance()
    }
}
