package com.ribbontek.ordermanagement.service.authentication

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AssociateSoftwareTokenRequest
import com.amazonaws.services.cognitoidp.model.AssociateSoftwareTokenResult
import com.amazonaws.services.cognitoidp.model.VerifySoftwareTokenRequest
import com.amazonaws.services.cognitoidp.model.VerifySoftwareTokenResult
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.ribbontek.grpccourse.TurnOnMfaResponse
import com.ribbontek.grpccourse.turnOnMfaResponse
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.grpc.model.VerifyMfaCommandModel
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.security.CustomAuthenticationToken
import com.ribbontek.ordermanagement.security.Principal
import com.ribbontek.ordermanagement.util.getPrincipal
import com.ribbontek.ordermanagement.util.toBase64String
import com.ribbontek.shared.result.TryResult
import com.ribbontek.shared.result.TryResultFailure
import com.ribbontek.shared.result.TryResultSuccess
import com.ribbontek.shared.result.toResult
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import java.io.ByteArrayOutputStream

@Validated
interface MfaService {
    fun turnOnMFA(principal: Principal = getPrincipal()): TurnOnMfaResponse
    fun verifyMFA(@Valid cmd: VerifyMfaCommandModel, principal: Principal = getPrincipal())
    fun turnOffMFA(principal: Principal = getPrincipal())
}

@Service
class MfaServiceImpl(
    private val cognitoIdentityProvider: AWSCognitoIdentityProvider,
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val userRepository: UserRepository
) : MfaService {
    private val log = logger()

    @Transactional(noRollbackFor = [AuthenticationException::class])
    override fun turnOnMFA(principal: Principal): TurnOnMfaResponse {
        val user = userRepository.findByEmail(principal.username)
            ?.takeIfValidAuthState()
            ?: throw AuthenticationException(INVALID_AUTHENTICATION_MSG)
        val newCode = setUpMfaForUser(principal).also {
            user.enableMfaLogin()
            log.info("Successfully enabled MFA for user ${principal.username}")
        }
        return turnOnMfaResponse {
            this.qrCode = newCode
        }
    }

    @Transactional(noRollbackFor = [AuthenticationException::class])
    override fun verifyMFA(cmd: VerifyMfaCommandModel, principal: Principal) {
        userRepository.findByEmail(principal.username)
            ?.takeIfValidAuthState()
            ?.takeIfValidMfaState()
            ?.verifyToken(cmd.totpCode)
            ?: throw AuthenticationException(INVALID_AUTHENTICATION_MSG)
    }

    @Transactional(noRollbackFor = [AuthenticationException::class])
    override fun turnOffMFA(principal: Principal) {
        userRepository.findByEmail(principal.username)
            ?.takeIfValidAuthState()
            ?.takeIfValidMfaState()
            ?.run {
                disableMfaLogin()
                log.info("Successfully disabled MFA for user ${principal.username}")
            }
            ?: throw AuthenticationException(INVALID_AUTHENTICATION_MSG)
    }

    private fun setUpMfaForUser(principal: Principal): String {
        val secretCode = associateTokenToRetrieveSecretCode()
        val code = cognitoAppClientConfig.getMfaUrl(principal.username, secretCode)
        return generateQrCodeZxing(code)
    }

    private fun generateQrCodeZxing(text: String): String {
        val qrCode = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 500, 500)
        val byteArrayOutputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(qrCode, "PNG", byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray().toBase64String()
    }

    private fun UserEntity.verifyToken(code: String) {
        val auth = SecurityContextHolder.getContext().authentication
        val tryResult = tryRun {
            cognitoIdentityProvider.verifySoftwareToken(
                VerifySoftwareTokenRequest()
                    .withAccessToken((auth as CustomAuthenticationToken).accessToken)
                    .withUserCode(code)
            )
        }
        trackLogin(tryResult)
        when (tryResult) {
            is TryResultSuccess<VerifySoftwareTokenResult> -> {
                log.info("Verified software token with user's access token")
            }

            is TryResultFailure -> {
                log.error("Unable to verify software token with user's access token", tryResult.exception)
                throw AuthenticationException(INVALID_AUTHENTICATION_MSG)
            }
        }
    }

    private fun associateTokenToRetrieveSecretCode(): String {
        val auth = SecurityContextHolder.getContext().authentication
        val tryResult = tryRun {
            cognitoIdentityProvider.associateSoftwareToken(
                AssociateSoftwareTokenRequest()
                    .withAccessToken((auth as CustomAuthenticationToken).accessToken)
            )
        }
        return when (tryResult) {
            is TryResultSuccess<AssociateSoftwareTokenResult> -> tryResult.toResult().secretCode
            is TryResultFailure -> {
                log.error("Unable to associated software token with user's access token", tryResult.exception)
                throw AuthenticationException(INVALID_AUTHENTICATION_MSG)
            }
        }
    }

    private fun UserEntity.takeIfValidAuthState(): UserEntity? {
        return when {
            idpUserName != null && idpStatus == "CONFIRMED" && !locked -> this
            else -> {
                trackLogin(TryResultFailure(AuthenticationException(INVALID_AUTHENTICATION_MSG)))
                null
            }
        }
    }

    private fun UserEntity.takeIfValidMfaState(): UserEntity? {
        return when {
            mfaSettings.enabled && mfaSettings.attempts < MAX_MFA_LOGIN_ATTEMPTS -> this
            else -> {
                trackLogin(TryResultFailure(AuthenticationException(INVALID_AUTHENTICATION_MSG)))
                null
            }
        }
    }

    private fun UserEntity.disableMfaLogin(): UserEntity {
        return userRepository.save(apply { mfaSettings.enabled = false })
    }

    private fun UserEntity.enableMfaLogin(): UserEntity {
        return this.run {
            if (!mfaSettings.enabled) {
                userRepository.save(apply { mfaSettings.enabled = true })
            } else {
                this
            }
        }
    }

    private fun UserEntity.trackLogin(result: TryResult<Throwable, Any>) {
        userRepository.save(
            when (result) {
                is TryResultSuccess -> this.apply { mfaSettings.attempts = 0 }
                is TryResultFailure ->
                    this.apply {
                        mfaSettings.attempts += 1
                        if (mfaSettings.attempts >= MAX_MFA_LOGIN_ATTEMPTS) locked = true
                    }
            }
        )
    }

    companion object {
        private const val MAX_MFA_LOGIN_ATTEMPTS = 50
        private const val INVALID_AUTHENTICATION_MSG = "Invalid authentication"
    }
}
