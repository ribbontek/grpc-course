package com.ribbontek.ordermanagement.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EntityScan(basePackages = ["com.ribbontek.ordermanagement.repository"])
@EnableJpaRepositories(basePackages = ["com.ribbontek.ordermanagement.repository"])
@EnableAsync(proxyTargetClass = true)
@EnableCaching(proxyTargetClass = true)
class ApplicationConfig
