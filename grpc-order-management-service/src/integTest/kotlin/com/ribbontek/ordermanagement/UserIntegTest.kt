package com.ribbontek.ordermanagement

import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.generator.UserGenerator
import com.ribbontek.ordermanagement.repository.user.RoleType.ADMIN
import com.ribbontek.ordermanagement.repository.user.UserRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class UserIntegTest : AbstractIntegTest() {
    @Autowired
    private lateinit var userGenerator: UserGenerator

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `generates an admin user`() {
        val (user, addresses) = userGenerator.generateAdmin()
        val userEntity = userRepository.findUserEntityByEmailWithEagerAddresses(user.email)
        assertNotNull(userEntity)
        assertNotNull(userEntity!!.addresses)
        assertThat(addresses.size, equalTo(userEntity.addresses.size))
    }

    @Test
    fun `generates a standard user`() {
        val (user, addresses) = userGenerator.generateStandard()
        val userEntity = userRepository.findUserEntityByEmailWithEagerAddresses(user.email)
        assertNotNull(userEntity)
        assertNotNull(userEntity!!.addresses)
        assertThat(addresses.size, equalTo(userEntity.addresses.size))
    }

    @Test
    fun `deletes a standard user`() {
        val (user, _) = userGenerator.generateStandard()
        assertNotNull(user)
        userRepository.delete(user)
        assertNull(userRepository.findByIdOrNull(user.id))
    }

    @Test
    fun `checks an admin user role`() {
        val (user, _) = userGenerator.generateAdmin()
        val userEntity = userRepository.findUserHavingRole(user.id!!, ADMIN.name)
        assertNotNull(userEntity)
    }
}
