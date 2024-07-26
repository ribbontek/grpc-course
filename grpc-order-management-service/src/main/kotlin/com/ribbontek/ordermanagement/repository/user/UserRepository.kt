package com.ribbontek.ordermanagement.repository.user

import com.ribbontek.ordermanagement.exception.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    @Query("select user from UserEntity user left join fetch user.addresses where user.email = :email")
    fun findUserEntityByEmailWithEagerAddresses(email: String): UserEntity?

    @Query("select user from UserEntity user where user.id = :userId and hasRole(:userId, :roleType) = true")
    fun findUserHavingRole(
        userId: Long,
        roleType: String
    ): UserEntity?

    fun findByIdpUserName(idpUserName: String): UserEntity?

    fun findByEmail(email: String): UserEntity?
}

fun UserRepository.expectOneById(userId: Long): UserEntity =
    findByIdOrNull(userId) ?: throw NotFoundException("Could not find user with user id $userId")

fun UserRepository.expectOneByEmail(email: String): UserEntity =
    findByEmail(email) ?: throw NotFoundException("Could not find user with email $email")
