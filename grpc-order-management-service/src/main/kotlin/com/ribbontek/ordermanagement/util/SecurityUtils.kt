package com.ribbontek.ordermanagement.util

import com.ribbontek.ordermanagement.security.Principal
import org.springframework.security.core.context.SecurityContextHolder

fun getPrincipal(): Principal = SecurityContextHolder.getContext().authentication.principal as Principal
