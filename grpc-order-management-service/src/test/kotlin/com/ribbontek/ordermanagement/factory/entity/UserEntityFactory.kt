package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.util.FakerUtil
import java.util.UUID

object UserEntityFactory {
    fun create(): UserEntity {
        return UserEntity(
            email = FakerUtil.email(),
            firstName = FakerUtil.firstName(),
            lastName = FakerUtil.lastName(),
            idpUserName = UUID.randomUUID().toString(),
            idpStatus = FakerUtil.status()
        )
    }
}
