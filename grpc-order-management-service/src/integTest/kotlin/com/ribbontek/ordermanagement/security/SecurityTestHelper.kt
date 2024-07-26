package com.ribbontek.ordermanagement.security

import com.ribbontek.ordermanagement.factory.AuthenticationFactory
import com.ribbontek.ordermanagement.factory.entity.RoleEntityFactory
import com.ribbontek.ordermanagement.grpc.model.LoginUserCommandModel
import com.ribbontek.ordermanagement.mapping.toRegisterUserCommandModel
import com.ribbontek.ordermanagement.repository.user.PolicyEntity
import com.ribbontek.ordermanagement.repository.user.RoleRepository
import com.ribbontek.ordermanagement.repository.user.RoleType.ADMIN
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneByEmail
import com.ribbontek.ordermanagement.service.authentication.DeactivateUserModel
import com.ribbontek.ordermanagement.service.authentication.DeactivateUserService
import com.ribbontek.ordermanagement.service.authentication.LoginUserService
import com.ribbontek.ordermanagement.service.authentication.RegisterUserService
import com.ribbontek.shared.result.tryRun
import io.grpc.Metadata
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

data class TestSecuredAuthentication(
    val userId: Long,
    val email: String,
    val password: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val authMetadata: Metadata
)

@Component
class SecurityTestHelper(
    private val registerUserService: RegisterUserService,
    private val loginUserService: LoginUserService,
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val deactivateUserService: DeactivateUserService,
    private val transactionTemplate: TransactionTemplate
) {
    fun withAdminUser(tests: (TestSecuredAuthentication) -> Unit) {
        val testSecuredAuthentication = registerAndLoginNewUser()
        migrateUserToAdmin(testSecuredAuthentication.email)
        try {
            tests(testSecuredAuthentication)
        } finally {
            deactivateUser(testSecuredAuthentication.email)
        }
    }

    fun withStandardUser(tests: (TestSecuredAuthentication) -> Unit) {
        val testSecuredAuthentication = registerAndLoginNewUser()
        try {
            tests(testSecuredAuthentication)
        } finally {
            deactivateUser(testSecuredAuthentication.email)
        }
    }

    fun deactivateUser(email: String) {
        tryRun { deactivateUserService.deactivateUser(DeactivateUserModel(email)) }
    }

    fun migrateUserToAdmin(email: String) {
        val user = userRepository.expectOneByEmail(email)
        transactionTemplate.executeWithoutResult {
            roleRepository.deleteAllByUser(user) // delete all STANDARD roles
        }
        roleRepository.save(
            RoleEntityFactory.create(
                userEntity = user,
                roleType = ADMIN,
                policies = listOf(
                    PolicyEntity(permission = "shopping:*"),
                    PolicyEntity(permission = "product:*"),
                    PolicyEntity(permission = "order:*"),
                    PolicyEntity(permission = "account:*"),
                    PolicyEntity(permission = "admin:*")
                )
            )
        )
    }

    private fun registerAndLoginNewUser(): TestSecuredAuthentication {
        val registerUserCmdModel = AuthenticationFactory.registerUserCommand().toRegisterUserCommandModel()
        registerUserService.registerUser(registerUserCmdModel)
        val loginResponse = loginUserService.loginUser(
            LoginUserCommandModel(
                username = registerUserCmdModel.email,
                password = registerUserCmdModel.password
            )
        )
        return TestSecuredAuthentication(
            userId = userRepository.expectOneByEmail(registerUserCmdModel.email).id!!,
            email = registerUserCmdModel.email,
            password = registerUserCmdModel.password,
            accessToken = loginResponse.accessToken,
            refreshToken = loginResponse.refreshToken,
            authMetadata = Metadata().apply {
                put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + loginResponse.accessToken)
            }
        )
    }
}
