package com.ribbontek.ordermanagement.repository.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<RoleEntity, Long> {
    @Query(
        """
        select role 
        from RoleEntity role 
        left join fetch role.user user 
        left join fetch role.policies policies 
        where user.id = :userId
        """
    )
    fun findAllByUserId(userId: Long): List<RoleEntity>

    fun existsByUser(user: UserEntity): Boolean

    @Query("delete RoleEntity re where re.user = :user")
    @Modifying
    fun deleteAllByUser(user: UserEntity)
}
