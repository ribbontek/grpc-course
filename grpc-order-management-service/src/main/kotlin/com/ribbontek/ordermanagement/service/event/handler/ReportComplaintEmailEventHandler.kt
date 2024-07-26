package com.ribbontek.ordermanagement.service.event.handler

import com.ribbontek.grpccourse.event.EventType.REPORT_COMPLAINT_EMAIL
import com.ribbontek.grpccourse.event.asyncDomainEvent
import com.ribbontek.ordermanagement.service.ReportComplaintEmailEvent
import com.ribbontek.ordermanagement.service.event.EventHandler
import com.ribbontek.ordermanagement.util.toBase64String
import com.ribbontek.shared.util.logger
import com.ribbontek.shared.util.toJson
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
@Profile("!integration")
class ReportComplaintEmailEventHandler(
    private val asyncQueueMessagingTemplate: QueueMessagingTemplate
) : EventHandler<ReportComplaintEmailEvent> {
    private val log = logger()

    @Async
    @EventListener
    override fun listen(event: ReportComplaintEmailEvent) {
        log.info("Processing event $event")
        asyncQueueMessagingTemplate.convertAndSend(
            asyncDomainEvent {
                this.type = REPORT_COMPLAINT_EMAIL
                this.data = event.toJson()
            }.toBase64String()
        )
        log.info("Triggered REPORT_COMPLAINT_EMAIL asyncDomainEvent to order-management-service queue")
    }
}
