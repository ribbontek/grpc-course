package com.ribbontek.ordermanagement.repository.abstracts

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
import java.io.Serializable
import java.time.ZonedDateTime

@MappedSuperclass
abstract class AbstractEntityDelete : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, updatable = false, nullable = false)
    val id: Long? = null

    @Basic(optional = false)
    @Column(updatable = false, nullable = false, name = "created_at")
    var createdAt: ZonedDateTime? = null

    @Column(insertable = false, name = "modified_at")
    var modifiedAt: ZonedDateTime? = null

    @Generated(event = [EventType.INSERT])
    @Column(insertable = false, nullable = false)
    var deleted = false

    @PrePersist
    fun prePersist() {
        this.createdAt = ZonedDateTime.now().toUtc()
    }

    @PreUpdate
    fun preUpdate() {
        this.modifiedAt = ZonedDateTime.now().toUtc()
    }
}
