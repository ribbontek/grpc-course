package com.ribbontek.ordermanagement.service.authentication

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserResult
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.MessageActionType.SUPPRESS
import com.amazonaws.services.cognitoidp.model.UserNotFoundException
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.grpc.model.ResetPasswordCommandModel
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneByEmail
import com.ribbontek.ordermanagement.repository.user.expectOneById
import com.ribbontek.ordermanagement.security.Principal
import com.ribbontek.shared.result.TryResultFailure
import com.ribbontek.shared.result.TryResultSuccess
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import kotlin.random.Random

@Validated
interface ResetPasswordService {
    fun resetPassword(principal: Principal)

    fun confirmResetPassword(
        @Valid cmd: ResetPasswordCommandModel
    )
}

@Service
class ResetPasswordServiceImpl(
    private val cognitoIdentityProvider: AWSCognitoIdentityProvider,
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val userRepository: UserRepository
) : ResetPasswordService {
    private val log = logger()

    @Transactional
    override fun resetPassword(principal: Principal) {
        userRepository.expectOneById(principal.userId).deleteUser(principal)
    }

    @Transactional
    override fun confirmResetPassword(cmd: ResetPasswordCommandModel) {
        userRepository.expectOneByEmail(cmd.email).resetPassword(cmd)
    }

    private fun UserEntity.resetPassword(cmd: ResetPasswordCommandModel) {
        if (!checkUserNotFoundInCognito(email)) {
            log.error("User with email $email already exists in Cognito")
            throw AuthenticationException("Invalid Authentication")
        }
        if (this.authCode == null || cmd.code != this.authCode) throw AuthenticationException("Invalid Authentication Code")
        tryRun {
            adminCreateUser(cmd)
            setUserPasswordAndRetrieveUser(cmd)
        }.onSuccess {
            userRepository.save(
                this@resetPassword.apply {
                    idpUserName = it.username
                    idpStatus = it.userStatus
                    locked = false
                    authCode = null
                    loginAttempts = 0
                }
            )
        }.onFailure {
            log.error("Failed to reset user password", it)
            throw AuthenticationException("Invalid Authentication")
        }
    }

    private fun UserEntity.deleteUser(principal: Principal) {
        if (checkUserNotFoundInCognito(email)) {
            log.error("User with email $email doesn't exists in Cognito")
            throw AuthenticationException("Invalid Authentication")
        }
        tryRun {
            adminDeleteUser(principal)
        }.onSuccess {
            userRepository.save(
                this@deleteUser.apply {
                    idpStatus = "DELETED"
                    locked = true
                    authCode = generateTimeBasedCode()
                    loginAttempts = 0
                }
            )
        }.onFailure {
            log.error("Failed to delete user", it)
            throw AuthenticationException("Invalid Authentication")
        }
    }

    private fun generateTimeBasedCode(): String {
        val random = Random(System.currentTimeMillis())
        return List(6) { random.nextInt(10) }.joinToString("")
    }

    private fun adminDeleteUser(principal: Principal): AdminDeleteUserResult {
        return cognitoIdentityProvider.adminDeleteUser(
            AdminDeleteUserRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(principal.idpUserName)
        )
    }

    private fun setUserPasswordAndRetrieveUser(cmd: ResetPasswordCommandModel): AdminGetUserResult {
        cognitoIdentityProvider.adminSetUserPassword(
            AdminSetUserPasswordRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(cmd.email)
                .withPassword(cmd.password)
                .withPermanent(true)
        )
        return adminGetUser(cmd.email)
    }

    private fun adminCreateUser(cmd: ResetPasswordCommandModel): AdminCreateUserResult {
        return cognitoIdentityProvider.adminCreateUser(
            AdminCreateUserRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(cmd.email)
                .withMessageAction(SUPPRESS)
                .withForceAliasCreation(false)
                .withUserAttributes(
                    AttributeType().withName("email").withValue(cmd.email),
                    AttributeType().withName("email_verified").withValue("true")
                )
        )
    }

    private fun checkUserNotFoundInCognito(email: String): Boolean {
        return when (val result = tryRun { adminGetUser(email) }) {
            is TryResultFailure -> result.exception is UserNotFoundException
            is TryResultSuccess -> false
        }
    }

    private fun adminGetUser(email: String): AdminGetUserResult =
        cognitoIdentityProvider.adminGetUser(
            AdminGetUserRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(email)
        )
}
