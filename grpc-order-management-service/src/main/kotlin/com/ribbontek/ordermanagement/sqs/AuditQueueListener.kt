package com.ribbontek.ordermanagement.sqs

import com.ribbontek.grpccourse.audit.AuditEvent
import com.ribbontek.grpccourse.audit.AuditEventType.CREATE
import com.ribbontek.grpccourse.audit.AuditEventType.DELETE
import com.ribbontek.grpccourse.audit.AuditEventType.UNRECOGNIZED
import com.ribbontek.grpccourse.audit.AuditEventType.UPDATE
import com.ribbontek.ordermanagement.repository.audit.AuditEntity
import com.ribbontek.ordermanagement.repository.audit.AuditEventType
import com.ribbontek.ordermanagement.repository.audit.AuditRepository
import com.ribbontek.ordermanagement.util.decodeBase64
import com.ribbontek.shared.result.onFailure
import com.ribbontek.shared.result.onSuccess
import com.ribbontek.shared.result.tryRun
import com.ribbontek.shared.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import com.ribbontek.grpccourse.audit.AuditEventType as GrpcAuditEventType

@Component
class AuditQueueListener(
    private val auditRepository: AuditRepository
) {
    private val log = logger()

    @ConditionalOnProperty(value = ["sqs.queue.audit.enabled"], havingValue = "true")
    @SqsListener("\${sqs.queue.audit.fifo.uri}", deletionPolicy = ON_SUCCESS)
    fun handleMessage(message: String) {
        log.info("Received Audit Event $message")
        tryRun {
            val auditEvent = AuditEvent.parseFrom(message.decodeBase64())
            log.info("Decoded Audit Event Successfully")
            auditRepository.save(auditEvent.toAuditEntity())
        }.onSuccess {
            log.info("Saved Audit Event Successfully")
        }.onFailure {
            log.error("Could not parse & save audit event", it)
            throw IllegalStateException("Could not parse & save audit event")
        }
    }

    private fun AuditEvent.toAuditEntity(): AuditEntity {
        return AuditEntity(
            eventId = id,
            type = type.toAuditEventTypeEntity(),
            event = if (hasEvent()) event else null,
            clazz = clazz,
            eventAtUtc = ZonedDateTime.parse(eventAtUtc)
        )
    }

    private fun GrpcAuditEventType.toAuditEventTypeEntity(): AuditEventType {
        return when (this) {
            CREATE -> AuditEventType.CREATE
            UPDATE -> AuditEventType.UPDATE
            DELETE -> AuditEventType.DELETE
            UNRECOGNIZED -> throw IllegalStateException("Invalid Event Type Provided: $this")
        }
    }
}
