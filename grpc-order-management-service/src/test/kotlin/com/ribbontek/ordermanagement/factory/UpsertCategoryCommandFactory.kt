package com.ribbontek.ordermanagement.factory

import com.ribbontek.grpccourse.admin.UpsertCategoryCommand
import com.ribbontek.grpccourse.admin.upsertCategoryCommand
import com.ribbontek.ordermanagement.util.FakerUtil

object UpsertCategoryCommandFactory {
    fun create(code: String? = null): UpsertCategoryCommand {
        return upsertCategoryCommand {
            code?.let { this.code = it } ?: run { this.code = FakerUtil.alphanumeric(50) }
            description = FakerUtil.alphanumeric(1000)
        }
    }
}
