package com.ribbontek.ordermanagement.service

interface DomainEvent

data class RegisterUserEvent(
    val userId: Long
) : DomainEvent

data class ReportComplaintEmailEvent(
    val userId: Long
) : DomainEvent

data class ReportBounceEmailEvent(
    val userId: Long
) : DomainEvent
