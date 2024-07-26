package com.ribbontek.ordermanagement.service.event

import com.ribbontek.grpccourse.audit.auditEvent
import com.ribbontek.ordermanagement.service.event.AuditEventType.CREATE
import com.ribbontek.ordermanagement.service.event.AuditEventType.DELETE
import com.ribbontek.ordermanagement.service.event.AuditEventType.UPDATE
import com.ribbontek.ordermanagement.util.toBase64String
import com.ribbontek.shared.util.logger
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID
import com.ribbontek.grpccourse.audit.AuditEvent as GrpcAuditEvent
import com.ribbontek.grpccourse.audit.AuditEventType as GrpcAuditEventType

@Service
@Profile("!integration")
class AuditEventHandler(
    private val fifoQueueMessagingTemplate: QueueMessagingTemplate
) : EventHandler<AuditEvent> {
    private val log = logger()

    @Async
    @EventListener
    override fun listen(event: AuditEvent) {
        log.info("processing event $event")
        fifoQueueMessagingTemplate.send(
            MessageBuilder.withPayload(event.toAuditEventGrpc().toBase64String())
                .setHeader("message-group-id", "AUDIT")
                .setHeader("message-deduplication-id", UUID.randomUUID().toString())
                .build()
        )
    }

    private fun AuditEvent.toAuditEventGrpc(): GrpcAuditEvent =
        let { source ->
            return auditEvent {
                this.type = source.type.toAuditEventTypeGrpc()
                this.id = source.id
                source.event?.let { this.event = it }
                this.clazz = source.clazz
                this.eventAtUtc = source.eventAtUtc
            }
        }

    private fun AuditEventType.toAuditEventTypeGrpc(): GrpcAuditEventType {
        return when (this) {
            CREATE -> GrpcAuditEventType.CREATE
            UPDATE -> GrpcAuditEventType.UPDATE
            DELETE -> GrpcAuditEventType.CREATE
        }
    }
}
