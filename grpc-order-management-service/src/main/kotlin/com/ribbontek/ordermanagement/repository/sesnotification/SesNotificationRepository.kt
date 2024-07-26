package com.ribbontek.ordermanagement.repository.sesnotification

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SesNotificationRepository : JpaRepository<SesNotificationEntity, Long> {
    fun findBySesMessageId(sesMessageId: String): SesNotificationEntity?
}
