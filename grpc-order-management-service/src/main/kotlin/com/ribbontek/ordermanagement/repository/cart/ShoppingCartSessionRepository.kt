package com.ribbontek.ordermanagement.repository.cart

import com.ribbontek.ordermanagement.exception.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import java.util.UUID

@Repository
interface ShoppingCartSessionRepository : JpaRepository<ShoppingCartSessionEntity, Long> {
    @Query(
        """
        select cart 
        from ShoppingCartSessionEntity cart 
        left join fetch cart.cartItems
        where cart.sessionId = :sessionId 
        and cart.user.id = :userId
        and cart.status <> 'CANCELLED'
        """
    )
    fun findBySessionIdAndUserId(
        sessionId: UUID,
        userId: Long
    ): ShoppingCartSessionEntity?

    fun existsBySessionIdAndUserId(
        sessionId: UUID,
        userId: Long
    ): Boolean

    fun existsBySessionId(sessionId: UUID): Boolean

    fun findAllByStatusInAndModifiedAtLessThanEqual(
        status: List<String>,
        modifiedAt: ZonedDateTime
    ): List<ShoppingCartSessionEntity>
}

fun ShoppingCartSessionRepository.expectOneBySessionIdAndUserId(
    sessionId: UUID,
    userId: Long
): ShoppingCartSessionEntity {
    return findBySessionIdAndUserId(sessionId, userId)
        ?: throw NotFoundException("Could not find shopping cart session with sessionId $sessionId and userId $userId")
}
