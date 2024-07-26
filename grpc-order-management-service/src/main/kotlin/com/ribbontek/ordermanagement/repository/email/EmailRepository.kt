package com.ribbontek.ordermanagement.repository.email

import com.ribbontek.ordermanagement.exception.NotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailRepository : JpaRepository<EmailEntity, Long> {
    fun findByUserIdAndTemplateName(
        userId: Long,
        templateName: String
    ): EmailEntity?

    fun findByMessageId(messageId: String): EmailEntity?
}

fun EmailRepository.expectOneByMessageId(messageId: String): EmailEntity =
    findByMessageId(messageId) ?: throw NotFoundException("Could not find email with messageId: $messageId")
