package com.ribbontek.ordermanagement.sqs

import com.ribbontek.grpccourse.audit.AuditEventType.CREATE
import com.ribbontek.grpccourse.audit.auditEvent
import com.ribbontek.ordermanagement.context.AbstractIntegTest
import com.ribbontek.ordermanagement.repository.audit.AuditEntity
import com.ribbontek.ordermanagement.repository.audit.AuditRepository
import com.ribbontek.ordermanagement.util.toBase64String
import com.ribbontek.shared.util.toJson
import com.ribbontek.shared.util.toUtc
import org.apache.commons.lang3.RandomStringUtils
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.messaging.support.MessageBuilder
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

class AuditQueueListenerTest : AbstractIntegTest() {
    @Autowired
    private lateinit var auditRepository: AuditRepository

    @Autowired
    private lateinit var auditQueueListener: AuditQueueListener

    @Autowired
    private lateinit var fifoQueueMessagingTemplate: QueueMessagingTemplate

    @Test
    fun `handle audit event - success`() {
        data class RandomData(val id: Long, val data: String)

        val eventData = RandomData(Random.nextLong(), RandomStringUtils.randomAlphabetic(10000))
        val auditEvent =
            auditEvent {
                this.type = CREATE
                this.id = Random.nextLong().toString()
                this.event = eventData.toJson()
                this.clazz = RandomData::class.java.simpleName
                this.eventAtUtc = ZonedDateTime.now().toUtc().toString()
            }

        fifoQueueMessagingTemplate.send(
            MessageBuilder.withPayload(auditEvent.toBase64String())
                .setHeader("message-group-id", "AUDIT")
                .setHeader("message-deduplication-id", UUID.randomUUID().toString())
                .build()
        )

        var auditEntity: AuditEntity? = null
        Awaitility.await().pollInSameThread().atMost(10, SECONDS).pollInterval(1, SECONDS).until {
            auditEntity =
                auditRepository.findAll().firstOrNull {
                    it.type.name == auditEvent.type.name &&
                        it.eventId == auditEvent.id &&
                        it.clazz == auditEvent.clazz
                }
            auditEntity != null
        }

        assertNotNull(auditEntity)
        assertThat(auditEntity!!.eventId, equalTo(auditEvent.id))
        assertThat(auditEntity!!.type.name, equalTo(auditEvent.type.name))
        assertNotNull(auditEntity!!.event)
        assertThat(auditEntity!!.event, equalTo(auditEvent.event))
        assertThat(auditEntity!!.clazz, equalTo(auditEvent.clazz))
        assertThat(auditEntity!!.eventAtUtc.toEpochSecond(), equalTo(ZonedDateTime.parse(auditEvent.eventAtUtc).toEpochSecond()))
    }

    @Test
    fun `handle audit event - UNRECOGNIZED - throws exception`() {
        val auditEvent =
            auditEvent {
                typeValue = -1
            }

        val exception = assertThrows<IllegalStateException> { auditQueueListener.handleMessage(auditEvent.toBase64String()) }
        assertThat(exception.message, equalTo("Could not parse & save audit event"))
    }
}
