package com.ribbontek.ordermanagement.sqs.handler

import com.ribbontek.grpccourse.event.AsyncDomainEvent
import com.ribbontek.grpccourse.event.EventType
import com.ribbontek.grpccourse.event.EventType.REPORT_COMPLAINT_EMAIL
import com.ribbontek.ordermanagement.repository.user.UserRepository
import com.ribbontek.ordermanagement.repository.user.expectOneById
import com.ribbontek.shared.util.fromJson
import org.springframework.stereotype.Component

@Component
class ReportComplaintEmailAsyncEventHandler(
    private val userRepository: UserRepository
) : AsyncEventHandler() {
    override val type: EventType = REPORT_COMPLAINT_EMAIL

    override fun handleEventImpl(event: AsyncDomainEvent) {
        if (!event.hasData()) throw IllegalStateException("Not data provided for event: $type")
        val eventData = event.data.fromJson(ReportComplaintEmail::class.java)
        val user = userRepository.expectOneById(eventData.userId)
        userRepository.save(user.apply { emailComplaint = true })
    }

    private data class ReportComplaintEmail(
        val userId: Long
    )
}
