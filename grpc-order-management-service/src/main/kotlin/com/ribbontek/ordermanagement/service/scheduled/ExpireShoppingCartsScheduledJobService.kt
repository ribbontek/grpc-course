package com.ribbontek.ordermanagement.service.scheduled

import com.ribbontek.ordermanagement.repository.cart.ShoppingCartSessionRepository
import com.ribbontek.ordermanagement.repository.cart.ShoppingCartStatus
import com.ribbontek.shared.util.toUtc
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class ExpireShoppingCartsScheduledJobService(
    private val shoppingCartSessionRepository: ShoppingCartSessionRepository,
    @Value("\${com.ribbontek.cron.enabled:true}") private val cronEnabled: Boolean
) : AbstractScheduledJobService() {
    override val JOB_NAME: String = "expire_shopping_carts_job"

    @Scheduled(cron = "\${com.ribbontek.cron.expire_shopping_carts_job}")
    fun expireShoppingCartsJob() {
        if (cronEnabled) {
            acquireLock {
                val sessions = shoppingCartSessionRepository.findAllByStatusInAndModifiedAtLessThanEqual(
                    listOf(ShoppingCartStatus.STARTED.name, ShoppingCartStatus.MODIFIED.name),
                    ZonedDateTime.now().minusHours(1).toUtc()
                )
                log.info("Found ${sessions.size} shopping cart sessions to cancel")
                shoppingCartSessionRepository.saveAll(sessions.map { it.apply { status = ShoppingCartStatus.CANCELLED.name } })
            }
        }
    }
}
