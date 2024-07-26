package com.ribbontek.ordermanagement.service.authentication

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AuthFlowType.USER_PASSWORD_AUTH
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult
import com.ribbontek.grpccourse.LoginResponse
import com.ribbontek.grpccourse.loginResponse
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.grpc.model.LoginUserCommandModel
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.shared.result.TryResult
import com.ribbontek.shared.result.TryResultFailure
import com.ribbontek.shared.result.TryResultSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Validated
interface LoginUserService {
    fun loginUser(@Valid cmd: LoginUserCommandModel): LoginResponse
}

@Service
class LoginUserServiceImpl(
    private val cognitoIdentityProvider: AWSCognitoIdentityProvider,
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val userRepository: UserRepository
) : LoginUserService {
    private val log = logger()

    @Transactional(noRollbackFor = [AuthenticationException::class])
    override fun loginUser(
        cmd: LoginUserCommandModel
    ): LoginResponse {
        val user = userRepository.findByEmail(cmd.username)?.takeIfValidAuthState() ?: throw AuthenticationException("Invalid username or password")
        when (val result = initiateAuth(cmd)) {
            is TryResultSuccess -> {
                user.trackLogin(result)
                return result.value.authenticationResult.let {
                    loginResponse {
                        this.accessToken = it.accessToken
                        this.idToken = it.idToken
                        this.refreshToken = it.refreshToken
                        this.expiresIn = it.expiresIn
                        this.tokenType = it.tokenType
                    }
                }
            }

            is TryResultFailure -> {
                user.trackLogin(result)
                log.error("Encountered exception during login", result.exception)
                throw AuthenticationException("Invalid username or password")
            }
        }
    }

    private fun UserEntity.takeIfValidAuthState(): UserEntity? {
        return when {
            idpUserName != null && idpStatus == "CONFIRMED" && !locked && loginAttempts < MAX_LOGIN_ATTEMPTS -> this
            else -> {
                trackLogin(TryResultFailure(AuthenticationException("Invalid username or password")))
                null
            }
        }
    }

    private fun UserEntity.trackLogin(result: TryResult<Throwable, InitiateAuthResult>) {
        userRepository.save(
            when (result) {
                is TryResultSuccess -> this.apply { loginAttempts = 0 }
                is TryResultFailure ->
                    this.apply {
                        loginAttempts += 1
                        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) locked = true
                    }
            }
        )
    }

    private fun initiateAuth(cmd: LoginUserCommandModel): TryResult<Throwable, InitiateAuthResult> {
        return tryRun {
            cognitoIdentityProvider.initiateAuth(
                InitiateAuthRequest()
                    .withAuthFlow(USER_PASSWORD_AUTH)
                    .withClientId(cognitoAppClientConfig.clientId)
                    .withAuthParameters(
                        mapOf(
                            "USERNAME" to cmd.username,
                            "PASSWORD" to cmd.password,
                            "SECRET_HASH" to calculateSecretHash(cmd.username)
                        )
                    )
            )
        }
    }

    private fun calculateSecretHash(email: String): String? {
        val hmacAlgorithm = "HmacSHA256"
        val key = SecretKeySpec(cognitoAppClientConfig.clientSecret.toByteArray(StandardCharsets.UTF_8), hmacAlgorithm)
        val mac = Mac.getInstance(hmacAlgorithm)
        mac.init(key)
        val hash = mac.doFinal("$email${cognitoAppClientConfig.clientId}".toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    companion object {
        private const val MAX_LOGIN_ATTEMPTS = 5
    }
}
