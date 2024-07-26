package com.ribbontek.ordermanagement.config

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.ribbontek.ordermanagement.config.EmailTemplateName.WELCOME_EMAIL
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

enum class EmailTemplateName {
    WELCOME_EMAIL
}

data class EmailTemplate(
    val title: String,
    val contentHtml: Resource
)

data class SesClientConfig(
    val region: String,
    val senderEmail: String,
    val headerImageUrl: String,
    val unsubscribeUrl: String,
    val emailTemplates: Map<EmailTemplateName, EmailTemplate>
)

@Configuration
class SesConfig(
    @Value("\${ses.region}") private val sesRegion: String,
    @Value("\${ses.sender}") private val senderEmail: String,
    @Value("\${ses.email.url.header}") private val headerImageUrl: String,
    @Value("\${ses.email.url.unsubscribe}") private val unsubscribeUrl: String,
    @Value("classpath:templates/welcome.html") private val welcomeEmail: Resource
) {
    @Bean
    fun amazonSimpleEmailService(): AmazonSimpleEmailService {
        return AmazonSimpleEmailServiceClient.builder()
            .withCredentials(DefaultAWSCredentialsProviderChain())
            .withRegion(sesRegion)
            .build()
    }

    @Bean
    fun sesClientConfig(): SesClientConfig {
        return SesClientConfig(
            region = sesRegion,
            senderEmail = senderEmail,
            headerImageUrl = headerImageUrl,
            unsubscribeUrl = unsubscribeUrl,
            emailTemplates = mapOf(
                WELCOME_EMAIL to EmailTemplate(title = "Welcome {{firstName}} to gRPC Course!", contentHtml = welcomeEmail)
            )
        )
    }
}
