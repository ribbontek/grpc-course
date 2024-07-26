package com.ribbontek.ordermanagement.service.security

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.repository.user.RoleRepository
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.security.CustomAuthenticationToken
import com.ribbontek.ordermanagement.security.Principal
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import io.grpc.Metadata
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date

interface SecurityService {
    fun authenticate(method: String, headers: Metadata, requiredPermissions: Array<String>)
}

@Service
class SecurityServiceImpl(
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val configurableJWTProcessor: ConfigurableJWTProcessor<*>,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : SecurityService {
    private val log = logger()

    /**
     * Authenticates the user token
     */
    override fun authenticate(method: String, headers: Metadata, requiredPermissions: Array<String>) {
        tryRun {
            // Extract JWT token from gRPC headers
            val token = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER))?.cleanToken()
                ?: throw AuthenticationException("Invalid Authentication")
            // parse the jwt token
            val jwtClaimsSet = configurableJWTProcessor.process(token, null)
            // validate the claims set
            if (!jwtClaimsSet.isValid()) throw AuthenticationException("Invalid Authentication")
            // retrieve the user
            val user = jwtClaimsSet.getStringClaim("username")?.let { userRepository.findByIdpUserName(it) }
                ?.takeIf {
                    it.idpStatus == "CONFIRMED" ||
                        (it.locked && it.idpStatus == "DELETED" && method == "ConfirmResetPassword")
                }
                ?: throw AuthenticationException("Invalid Authentication")
            // retrieve the user roles
            val userPermissions = roleRepository.findAllByUserId(user.id!!)
                .flatMap { role -> role.policies.map { it.permission } }
                .toSet()
            // Check if the authenticated user has the required permissions
            val hasPermissions = userPermissions.checkPermissions(requiredPermissions)
            if (!hasPermissions) throw AuthenticationException("Invalid Authentication")
            // Set the authentication in the SecurityContext & attach to the session
            SecurityContextHolder.getContext().authentication = CustomAuthenticationToken(
                accessToken = token,
                principal = Principal(
                    username = user.email,
                    userId = user.id,
                    idpUserName = user.idpUserName,
                    authorities = userPermissions.map { SimpleGrantedAuthority(it) }
                )
            )
            log.info("Verified OAuth token for user with email: ${user.email}")
        }.onFailure {
            log.error("Caught exception authenticating user JWT token", it)
            throw AuthenticationException("Invalid Authentication")
        }
    }

    // validates the issuer & the expiration time
    private fun JWTClaimsSet.isValid() =
        issuer.equals(cognitoAppClientConfig.getCognitoIdentityPoolUrl()) || expirationTime?.after(Date(Instant.now().toEpochMilli())) == true

    private fun String.cleanToken(): String {
        if (!this.startsWith("Bearer ")) throw AuthenticationException("Invalid Authentication")
        return this.replace("Bearer ", "")
    }

    private fun Set<String>.checkPermissions(requiredPermissions: Array<String>): Boolean =
        requiredPermissions.any { this.hasMatchingPermission(it) }

    private fun Set<String>.hasMatchingPermission(requiredPermission: String): Boolean {
        return this.any { permission ->
            permission == requiredPermission ||
                (permission.contains('*') && permission.permissionToRegex().matches(requiredPermission))
        }
    }

    private fun String.permissionToRegex(): Regex = Regex(this.replace("*", "[\\w:]+"))
}
