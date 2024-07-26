package com.ribbontek.ordermanagement.config

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.util.ResourceRetriever
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL

data class CognitoAppClientConfig(
    val region: String,
    val clientId: String,
    val clientSecret: String,
    val userPoolId: String,
    val mfaAppName: String
) {
    fun getJwkUrl(): String {
        return String.format(COGNITO_IDENTITY_POOL_URL + JSON_WEB_TOKEN_SET_URL_SUFFIX, region, userPoolId)
    }

    fun getCognitoIdentityPoolUrl(): String {
        return String.format(COGNITO_IDENTITY_POOL_URL, region, userPoolId)
    }

    fun getMfaUrl(email: String, secretCode: String): String {
        // could build a proxy for the cognito identity pool url, so it's not exposed as MFA auth
        return String.format(MFA_URL, mfaAppName, email, secretCode, getCognitoIdentityPoolUrl())
    }

    companion object {
        private const val COGNITO_IDENTITY_POOL_URL = "https://cognito-idp.%s.amazonaws.com/%s"
        private const val JSON_WEB_TOKEN_SET_URL_SUFFIX = "/.well-known/jwks.json"
        private const val MFA_URL = "otpauth://totp/%s:%s?secret=%s&issuer=%s"
    }
}

@Configuration
class CognitoConfig(
    @Value("\${cognito.region}") private val region: String,
    @Value("\${cognito.client.id}") private val clientId: String,
    @Value("\${cognito.client.secret}") private val clientSecret: String,
    @Value("\${cognito.mfa.app.name}") private val mfaAppName: String,
    @Value("\${cognito.pool.id}") private val userPoolId: String
) {
    @Bean
    fun cognitoIdentityProvider(): AWSCognitoIdentityProvider {
        return AWSCognitoIdentityProviderClient.builder()
            .withCredentials(DefaultAWSCredentialsProviderChain())
            .withClientConfiguration(
                ClientConfiguration().withRetryPolicy(
                    PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(3)
                )
            )
            .withRegion(region)
            .build()
    }

    @Bean
    fun cognitoAppClientConfig(): CognitoAppClientConfig {
        return CognitoAppClientConfig(
            region = region,
            clientId = clientId,
            clientSecret = clientSecret,
            userPoolId = userPoolId,
            mfaAppName = mfaAppName
        )
    }

    @Bean
    fun configurableJWTProcessor(cognitoAppClientConfig: CognitoAppClientConfig): ConfigurableJWTProcessor<*> {
        val resourceRetriever: ResourceRetriever = DefaultResourceRetriever(2000, 2000)
        val jwkSetURL = URL(cognitoAppClientConfig.getJwkUrl())
        val keySource = JWKSourceBuilder.create<SecurityContext>(jwkSetURL, resourceRetriever).build()
        return DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource)
        }
    }
}
