package com.ribbontek.ordermanagement.context

import com.ribbontek.ordermanagement.security.SecurityTestHelper
import com.ribbontek.ordermanagement.security.TestSecuredAuthentication
import com.ribbontek.shared.util.logger
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration
import net.devh.boot.grpc.client.config.GrpcChannelsProperties
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration
import net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import javax.inject.Inject

@GrpcSpringBootTest
@ImportAutoConfiguration(
    GrpcServerAutoConfiguration::class,
    GrpcServerFactoryAutoConfiguration::class,
    GrpcClientAutoConfiguration::class
)
@EnableConfigurationProperties(GrpcChannelsProperties::class)
abstract class AbstractIntegTest {

    protected val log = logger()

    @Inject
    protected lateinit var securityTestHelper: SecurityTestHelper

    fun withAdminUser(tests: TestSecuredAuthentication.() -> Unit) {
        securityTestHelper.withAdminUser { tests(it) }
    }

    fun withStandardUser(tests: TestSecuredAuthentication.() -> Unit) {
        securityTestHelper.withStandardUser { tests(it) }
    }
}
