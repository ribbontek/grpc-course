package com.ribbontek.ordermanagement.security

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
annotation class RequiresOAuthPermission(
    vararg val value: String
)
