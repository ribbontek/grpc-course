package com.ribbontek.ordermanagement.service.authentication

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult
import com.amazonaws.services.cognitoidp.model.AdminSetUserPasswordRequest
import com.amazonaws.services.cognitoidp.model.AttributeType
import com.amazonaws.services.cognitoidp.model.MessageActionType.SUPPRESS
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.grpc.model.RegisterUserCommandModel
import com.ribbontek.ordermanagement.mapping.toUserEntity
import com.ribbontek.ordermanagement.repository.user.PolicyEntity
import com.ribbontek.ordermanagement.repository.user.RoleEntity
import com.ribbontek.ordermanagement.repository.user.RoleRepository
import com.ribbontek.ordermanagement.repository.user.RoleType.STANDARD
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.service.DomainEventPublisher
import com.ribbontek.ordermanagement.service.RegisterUserEvent
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated

@Validated
interface RegisterUserService {
    fun registerUser(
        @Valid cmd: RegisterUserCommandModel
    )
}

@Service
class RegisterUserServiceImpl(
    private val cognitoIdentityProvider: AWSCognitoIdentityProvider,
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) : RegisterUserService {
    private val log = logger()

    @Transactional
    override fun registerUser(cmd: RegisterUserCommandModel) {
        val userEntity = userRepository.findByEmail(cmd.email)?.update(cmd) ?: createAndUpdate(cmd)
        userEntity.triggerRegisterUserEvent()
    }

    private fun UserEntity.triggerRegisterUserEvent() =
        also {
            if (it.idpStatus == "CONFIRMED") DomainEventPublisher.publishEvent(RegisterUserEvent(userId = it.id!!))
        }

    private fun UserEntity.update(registerUserCommand: RegisterUserCommandModel): UserEntity {
        if (idpStatus != "FORCE_CHANGE_PASSWORD") return this
        tryRun {
            setUserPasswordAndRetrieveUser(registerUserCommand)
        }.onSuccess {
            updateUserDetails(it)
            assignStandardUserRoles()
        }.onFailure {
            log.error("Failed to update user details with password", it)
            throw AuthenticationException("Invalid Authentication")
        }
        return this
    }

    private fun createAndUpdate(registerUserCommand: RegisterUserCommandModel): UserEntity {
        var userEntity: UserEntity? = null
        tryRun {
            adminCreateUser(registerUserCommand)
        }.onSuccess { adminCreateUserResult ->
            userEntity = userRepository.save(
                registerUserCommand.toUserEntity()
                    .apply {
                        idpUserName = adminCreateUserResult.user.username
                        idpStatus = adminCreateUserResult.user.userStatus
                    }
            ).update(registerUserCommand)
        }.onFailure {
            log.error("Failed to create user details with password", it)
            throw AuthenticationException("Invalid Authentication")
        }
        return userEntity ?: throw AuthenticationException("Invalid Authentication")
    }

    private fun UserEntity.updateUserDetails(adminGetUserResult: AdminGetUserResult): UserEntity {
        return userRepository.save(
            this.apply {
                idpUserName = adminGetUserResult.username
                idpStatus = adminGetUserResult.userStatus
            }
        )
    }

    private fun UserEntity.assignStandardUserRoles(): UserEntity {
        if (!roleRepository.existsByUser(this)) {
            roleRepository.save(
                RoleEntity(
                    user = this,
                    roleType = STANDARD,
                    policies = mutableSetOf(
                        PolicyEntity(permission = "shopping:*"),
                        PolicyEntity(permission = "product:*"),
                        PolicyEntity(permission = "order:*"),
                        PolicyEntity(permission = "account:*")
                    )
                )
            )
        }
        return this
    }

    private fun setUserPasswordAndRetrieveUser(registerUserCommand: RegisterUserCommandModel): AdminGetUserResult {
        cognitoIdentityProvider.adminSetUserPassword(
            AdminSetUserPasswordRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(registerUserCommand.email)
                .withPassword(registerUserCommand.password)
                .withPermanent(true)
        )
        return cognitoIdentityProvider.adminGetUser(
            AdminGetUserRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(registerUserCommand.email)
        )
    }

    private fun adminCreateUser(registerUserCommand: RegisterUserCommandModel): AdminCreateUserResult {
        return cognitoIdentityProvider.adminCreateUser(
            AdminCreateUserRequest()
                .withUserPoolId(cognitoAppClientConfig.userPoolId)
                .withUsername(registerUserCommand.email)
                .withMessageAction(SUPPRESS)
                .withForceAliasCreation(false)
                .withUserAttributes(
                    AttributeType().withName("email").withValue(registerUserCommand.email),
                    AttributeType().withName("email_verified").withValue("true")
                )
        )
    }
}
