package com.ribbontek.ordermanagement.service.authentication

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest
import com.ribbontek.ordermanagement.config.CognitoAppClientConfig
import com.ribbontek.ordermanagement.exception.AuthenticationException
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneByEmail
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class DeactivateUserModel(val email: String)

interface DeactivateUserService {
    fun deactivateUser(cmd: DeactivateUserModel)
}

@Service
class DeactivateUserServiceImpl(
    private val cognitoIdentityProvider: AWSCognitoIdentityProvider,
    private val cognitoAppClientConfig: CognitoAppClientConfig,
    private val userRepository: UserRepository
) : DeactivateUserService {
    private val log = logger()

    @Transactional
    override fun deactivateUser(cmd: DeactivateUserModel) {
        val user = userRepository.expectOneByEmail(cmd.email)
        tryRun {
            cognitoIdentityProvider.adminDeleteUser(
                AdminDeleteUserRequest()
                    .withUserPoolId(cognitoAppClientConfig.userPoolId)
                    .withUsername(user.email)
            )
        }.onSuccess {
            userRepository.delete(user)
        }.onFailure {
            log.error("Failed to delete user", it)
            throw AuthenticationException("Invalid Authentication")
        }
    }
}
