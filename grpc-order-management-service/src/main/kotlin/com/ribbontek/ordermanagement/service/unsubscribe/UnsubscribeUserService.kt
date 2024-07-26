package com.ribbontek.ordermanagement.service.unsubscribe

import com.ribbontek.ordermanagement.exception.BadRequestException
import com.ribbontek.ordermanagement.grpc.model.UnsubscribeUserCommandModel
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneByEmail
import jakarta.validation.Valid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated

@Validated
interface UnsubscribeUserService {
    fun unsubscribeUser(
        @Valid cmd: UnsubscribeUserCommandModel
    )
}

@Service
class UnsubscribeUserServiceImpl(
    private val userRepository: UserRepository
) : UnsubscribeUserService {
    @Transactional
    override fun unsubscribeUser(cmd: UnsubscribeUserCommandModel) {
        val user = userRepository.expectOneByEmail(cmd.email)
        when (user.unsubscribeCode?.equals(cmd.code)) {
            true -> userRepository.save(user.apply { unsubscribed = true })
            else -> throw BadRequestException("Invalid code found for user with email ${cmd.email}")
        }
    }
}
