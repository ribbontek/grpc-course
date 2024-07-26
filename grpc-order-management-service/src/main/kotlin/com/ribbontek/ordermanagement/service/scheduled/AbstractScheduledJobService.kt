package com.ribbontek.ordermanagement.service.scheduled

import com.ribbontek.shared.util.logger
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.inject.Inject

abstract class AbstractScheduledJobService {
    protected val log = logger()

    protected abstract val JOB_NAME: String

    @Inject
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    companion object {
        private const val LOCK_JOB = """
            update scheduled_job_lock 
            set locked = true 
            where locked = false 
            and name = :name  
        """
        private const val UNLOCK_JOB = """
            update scheduled_job_lock 
            set locked = false 
            where name = :name  
        """
    }

    fun acquireLock(block: () -> Unit) {
        try {
            if (jdbcTemplate.update(LOCK_JOB, mapOf("name" to JOB_NAME)) > 0) {
                log.info("Starting scheduled job: $JOB_NAME")
                block()
            }
        } catch (ex: Exception) {
            log.error("Caught exception processing $JOB_NAME", ex)
        } finally {
            jdbcTemplate.update(UNLOCK_JOB, mapOf("name" to JOB_NAME))
            log.info("Finishing scheduled job: $JOB_NAME")
        }
    }
}
