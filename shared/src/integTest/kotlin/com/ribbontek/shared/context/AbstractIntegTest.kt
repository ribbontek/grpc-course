package com.ribbontek.shared.context

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@ActiveProfiles("integration")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = [TestApplication::class])
abstract class AbstractIntegTest

@SpringBootApplication
@EnableJpaRepositories("com.ribbontek.shared.repository")
@EntityScan(basePackages = ["com.ribbontek.shared.repository"])
class TestApplication {
    fun main(args: Array<String>) {
        runApplication<TestApplication>(*args)
    }
}
