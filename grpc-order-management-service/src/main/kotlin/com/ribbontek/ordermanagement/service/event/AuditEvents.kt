package com.ribbontek.ordermanagement.service.event

import com.ribbontek.ordermanagement.service.DomainEvent
import com.ribbontek.ordermanagement.service.event.AuditEventType.CREATE
import com.ribbontek.ordermanagement.service.event.AuditEventType.UPDATE
import com.ribbontek.shared.util.toJson
import com.ribbontek.shared.util.toUtc
import java.time.ZonedDateTime

enum class AuditEventType {
    CREATE,
    UPDATE,
    DELETE
}

data class AuditEvent(
    val id: String,
    val type: AuditEventType,
    val event: String? = null,
    val clazz: String,
    val eventAtUtc: String
) : DomainEvent

interface Auditable {
    val id: Long?
    fun postPersist()
    fun postUpdate()
    fun postRemove()
}

fun <T : Auditable> T.toAuditEvent(type: AuditEventType): AuditEvent {
    return AuditEvent(
        id = this.id.toString(),
        type = type,
        event = when (type) {
            in listOf(CREATE, UPDATE) -> this.toJson()
            else -> null
        },
        clazz = this::class.simpleName!!,
        eventAtUtc = ZonedDateTime.now().toUtc().toString()
    )
}
