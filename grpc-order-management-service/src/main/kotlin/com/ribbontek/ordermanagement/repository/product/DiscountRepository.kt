package com.ribbontek.ordermanagement.repository.product

import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.repository.abstracts.AdminEntityDeleteRepository
import com.ribbontek.shared.specdsl.equal
import com.ribbontek.shared.specdsl.greaterThanOrEqualTo
import com.ribbontek.shared.specdsl.lessThan
import com.ribbontek.shared.specdsl.lessThanOrEqualTo
import com.ribbontek.shared.specdsl.like
import com.ribbontek.shared.util.atStartOfDay
import com.ribbontek.shared.util.toUtc
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.ZonedDateTime

@Repository
interface DiscountRepository : AdminEntityDeleteRepository<DiscountEntity>, JpaSpecificationExecutor<DiscountEntity> {
    @Query("select d from DiscountEntity d where lower(d.code) = lower(:code) and d.deleted = false")
    fun findByCode(code: String): DiscountEntity?
}

fun DiscountRepository.expectOneByCode(code: String): DiscountEntity {
    return findByCode(code) ?: throw NotFoundException("Could not find discount with code: $code")
}

object DiscountEntitySpecs {
    fun hasAmountRangeStart(amountRangeStart: BigDecimal?): Specification<DiscountEntity>? =
        amountRangeStart?.let {
            DiscountEntity::amount.greaterThanOrEqualTo(amountRangeStart)
        }

    fun hasAmountRangeEnd(amountRangeEnd: BigDecimal?): Specification<DiscountEntity>? =
        amountRangeEnd?.let {
            DiscountEntity::amount.lessThanOrEqualTo(amountRangeEnd)
        }

    fun hasCodeLike(code: String?): Specification<DiscountEntity>? =
        code?.let {
            DiscountEntity::code.like(it)
        }

    fun hasDeleted(deleted: Boolean?): Specification<DiscountEntity>? =
        deleted?.let {
            DiscountEntity::deleted.equal(it)
        }

    fun hasExpired(expired: Boolean?): Specification<DiscountEntity>? =
        expired?.let {
            val today = ZonedDateTime.now().toUtc().atStartOfDay()
            if (it) {
                DiscountEntity::expiresAt.lessThan(today)
            } else {
                DiscountEntity::expiresAt.greaterThanOrEqualTo(today)
            }
        }
}
