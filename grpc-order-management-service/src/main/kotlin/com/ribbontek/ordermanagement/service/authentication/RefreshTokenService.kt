package com.ribbontek.ordermanagement.service.authentication

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AuthFlowType.REFRESH_TOKEN_AUTH
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult
import com.ribbontek.grpccourse.LoginResponse
import com.ribbontek.grpccourse.loginResponse
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.grpc.model.RefreshTokenCommandModel
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.security.Principal
import com.ribbontek.ordermanagement.util.getPrincipal
import com.ribbontek.shared.result.TryResult
import com.ribbontek.shared.result.TryResultFailure
import com.ribbontek.shared.result.TryResultSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import jakarta.validation.Valid
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Validated
interface RefreshTokenService {
    fun refreshToken(
        @Valid cmd: RefreshTokenCommandModel,
        principal: Principal = getPrincipal()
    ): LoginResponse
}

@Service
class RefreshTokenServiceImpl(
    private val cognitoIdentityProvider: AWSCognitoIdentityProvider,
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val userRepository: UserRepository
) : RefreshTokenService {
    private val log = logger()

    @Transactional
    override fun refreshToken(cmd: RefreshTokenCommandModel, principal: Principal): LoginResponse {
        userRepository.findByIdOrNull(principal.userId)?.takeIfValidAuthState()
            ?: throw AuthenticationException("Invalid Authentication")
        when (val result = initiateAuth(cmd, principal)) {
            is TryResultSuccess -> {
                return result.value.authenticationResult.let {
                    loginResponse {
                        this.accessToken = it.accessToken
                        this.idToken = it.idToken
                        this.refreshToken = cmd.refreshToken
                        this.expiresIn = it.expiresIn
                        this.tokenType = it.tokenType
                    }
                }
            }
            is TryResultFailure -> {
                log.error("Encountered exception during login", result.exception)
                throw AuthenticationException("Invalid Authentication")
            }
        }
    }

    private fun UserEntity.takeIfValidAuthState(): UserEntity? {
        return this.takeIf {
            it.idpUserName != null && it.idpStatus == "CONFIRMED" && !it.locked && it.loginAttempts < MAX_LOGIN_ATTEMPTS
        }
    }

    private fun initiateAuth(cmd: RefreshTokenCommandModel, principal: Principal): TryResult<Throwable, InitiateAuthResult> {
        return tryRun {
            cognitoIdentityProvider.initiateAuth(
                InitiateAuthRequest()
                    .withAuthFlow(REFRESH_TOKEN_AUTH)
                    .withClientId(cognitoAppClientConfig.clientId)
                    .withAuthParameters(
                        mapOf(
                            "REFRESH_TOKEN" to cmd.refreshToken,
                            "SECRET_HASH" to calculateSecretHash(principal.idpUserName!!)
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
