package com.ribbontek.ordermanagement.repository.abstracts

import com.ribbontek.ordermanagement.security.Principal
import com.ribbontek.shared.util.toUtc
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.hibernate.annotations.Generated
import org.hibernate.generator.EventType
import org.springframework.security.core.context.SecurityContextHolder
import java.io.Serializable
import java.time.ZonedDateTime

@MappedSuperclass
abstract class AbstractAdminEntityDelete : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, updatable = false, nullable = false)
    val id: Long? = null

    @Basic(optional = false)
    @Column(updatable = false, nullable = false, name = "created_at")
    var createdAt: ZonedDateTime? = null

    @Column(updatable = false, nullable = false, name = "created_by")
    var createdBy: String? = null

    @Column(insertable = false, name = "modified_at")
    var modifiedAt: ZonedDateTime? = null

    @Column(insertable = false, name = "modified_by")
    var modifiedBy: String? = null

    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, nullable = false)
    var deleted = false

    @PrePersist
    fun prePersist() {
        this.createdBy = (SecurityContextHolder.getContext().authentication.principal as Principal).username
        this.createdAt = ZonedDateTime.now().toUtc()
    }

    @PreUpdate
    fun preUpdate() {
        this.modifiedBy = (SecurityContextHolder.getContext().authentication.principal as Principal).username
        this.modifiedAt = ZonedDateTime.now().toUtc()
    }
}
