package com.ribbontek.ordermanagement.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.core.context.SecurityContext as SpringSecurityContext

/**
 * The TestSecurityContext utility helps construct Spring's security context
 *
 * To be used in service / component tests
 */
object TestSecurityContext {
    fun principal(
        email: String,
        userId: Long,
        idpUserName: String,
        authorities: Set<String> = emptySet()
    ): Principal {
        return Principal(
            username = email,
            userId = userId,
            idpUserName = idpUserName,
            authorities = authorities.toGrantedAuthorities()
        )
    }

    fun unauthenticated() {
        SecurityContextHolder.getContext().authentication = null
    }

    fun configureMockUser(
        userId: Long = 1,
        username: String = "testuser",
        idpUserName: String = "testuser",
        authorities: Set<String> = emptySet()
    ) {
        val principal = principal(
            email = username,
            userId = userId,
            idpUserName = idpUserName,
            authorities = authorities
        )
        val securityContext = SecurityContextImpl().apply {
            authentication =
                CustomAuthenticationToken(
                    accessToken = null,
                    principal = principal,
                    authorities = authorities.toGrantedAuthorities()
                )
        }
        updateSpringSecurityContext(securityContext)
    }

    private fun updateSpringSecurityContext(securityContext: SpringSecurityContext) =
        TestSecurityContextHolder.setContext(securityContext)

    private fun Set<String>.toGrantedAuthorities() = this.mapTo(arrayListOf(), ::SimpleGrantedAuthority)
}
