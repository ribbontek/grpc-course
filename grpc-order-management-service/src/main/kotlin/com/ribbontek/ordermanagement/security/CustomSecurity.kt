package com.ribbontek.ordermanagement.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class Principal(
    username: String,
    val userId: Long,
    val idpUserName: String? = null,
    authorities: Collection<GrantedAuthority> = hashSetOf()
) : User(username, "", authorities)

class CustomAuthenticationToken(
    val accessToken: String? = null,
    val principal: Principal? = null,
    authorities: Collection<GrantedAuthority>? = emptySet()
) : AbstractAuthenticationToken(authorities) {
    init {
        this.isAuthenticated = true
    }

    override fun getCredentials() = accessToken

    override fun getPrincipal(): Any? = principal
}
