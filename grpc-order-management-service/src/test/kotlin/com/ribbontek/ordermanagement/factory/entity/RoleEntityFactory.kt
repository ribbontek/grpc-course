package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.user.PolicyEntity
import com.ribbontek.ordermanagement.repository.user.RoleEntity
import com.ribbontek.ordermanagement.repository.user.RoleType
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object RoleEntityFactory {
    fun create(
        userEntity: UserEntity,
        roleType: RoleType? = null,
        policies: List<PolicyEntity> = emptyList()
    ): RoleEntity {
        return RoleEntity(
            roleType = roleType ?: FakerUtil.enum(),
            user = userEntity,
            policies = policies.toMutableSet()
        )
    }
}
