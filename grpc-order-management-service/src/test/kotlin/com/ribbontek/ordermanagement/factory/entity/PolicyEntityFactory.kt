package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.user.PolicyEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object PolicyEntityFactory {
    fun create(): PolicyEntity {
        return PolicyEntity(
            permission = FakerUtil.alphanumeric()
        )
    }
}
