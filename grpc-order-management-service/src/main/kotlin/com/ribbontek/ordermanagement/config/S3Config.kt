package com.ribbontek.ordermanagement.config

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config(
    @Value("\${com.ribbontek.s3.region}") private val s3Region: String
) {
    @Bean
    fun s3Client(): AmazonS3Client =
        AmazonS3ClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain())
            .withRegion(s3Region)
            .withClientConfiguration(
                ClientConfiguration().withRetryPolicy(
                    PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(3)
                )
            )
            .build() as AmazonS3Client
}
