package com.ribbontek.ordermanagement.repository.order

import com.ribbontek.ordermanagement.exception.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<OrderEntity, Long> {
    @Query(
        """
        select orderEntity 
        from OrderEntity orderEntity 
        left join fetch orderEntity.orderItems
        left join fetch orderEntity.addresses
        left join fetch orderEntity.payment
        where orderEntity.sessionId = :sessionId and orderEntity.user.id = :userId
        """
    )
    fun findEagerBySessionIdAndUserId(
        sessionId: UUID,
        userId: Long
    ): OrderEntity?

    fun existsBySessionIdAndUserId(
        sessionId: UUID,
        userId: Long
    ): Boolean

    fun existsBySessionId(sessionId: UUID): Boolean
}

fun OrderRepository.expectOneBySessionIdAndUserId(
    sessionId: UUID,
    userId: Long
): OrderEntity {
    return findEagerBySessionIdAndUserId(sessionId, userId)
        ?: throw NotFoundException("Could not find order with sessionId $sessionId and userId $userId")
}
