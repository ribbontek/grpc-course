package com.ribbontek.ordermanagement.repository.audit

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuditRepository : JpaRepository<AuditEntity, Long>
