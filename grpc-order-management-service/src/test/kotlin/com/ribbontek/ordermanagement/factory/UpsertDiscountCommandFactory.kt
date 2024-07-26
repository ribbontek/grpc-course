package com.ribbontek.ordermanagement.factory

import com.ribbontek.grpccourse.admin.UpsertDiscountCommand
import com.ribbontek.grpccourse.admin.upsertDiscountCommand
import com.ribbontek.ordermanagement.util.FakerUtil
import com.ribbontek.shared.util.toUtc
import java.time.ZonedDateTime

object UpsertDiscountCommandFactory {
    fun create(code: String? = null): UpsertDiscountCommand {
        return upsertDiscountCommand {
            amount = FakerUtil.price().toFloat()
            this.code = code ?: FakerUtil.alphanumeric(50)
            description = FakerUtil.alphanumeric(255)
            expiresAtUtc = ZonedDateTime.now().plusDays(1).toUtc().toString()
        }
    }
}
