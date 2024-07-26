package com.ribbontek.ordermanagement.repository.user

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntityDelete
import com.ribbontek.ordermanagement.service.DomainEventPublisher
import com.ribbontek.ordermanagement.service.event.AuditEventType.CREATE
import com.ribbontek.ordermanagement.service.event.AuditEventType.DELETE
import com.ribbontek.ordermanagement.service.event.AuditEventType.UPDATE
import com.ribbontek.ordermanagement.service.event.Auditable
import com.ribbontek.ordermanagement.service.event.toAuditEvent
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.MapKeyJoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.SQLDelete

@Embeddable
data class MfaSettings(
    @Column(name = "mfa_enabled", nullable = false)
    var enabled: Boolean = false,
    @Column(name = "mfa_login_required", nullable = false)
    var loginRequired: Boolean = false,
    @Column(name = "mfa_login_attempts", nullable = false)
    var attempts: Int = 0
)

@Entity
@Table(name = "vw_user")
@AttributeOverride(name = "id", column = Column(name = "user_id"))
@SQLDelete(sql = "update vw_user set deleted = true where user_id = ?")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class UserEntity(
    @Column(nullable = false, length = 255)
    val email: String,
    @Column(nullable = false, length = 255)
    var firstName: String,
    @Column(nullable = false, length = 255)
    var lastName: String,
    @Column(length = 255)
    var idpUserName: String? = null,
    @Column(length = 255)
    var idpStatus: String? = null,
    @Embedded
    var mfaSettings: MfaSettings = MfaSettings(),
    @Column(nullable = false)
    var locked: Boolean = false,
    // TODO: Could add in email settings here
    @Column(nullable = false)
    var unsubscribed: Boolean = false,
    @Column
    var unsubscribeCode: String? = null,
    @Column(nullable = false)
    var emailBounced: Boolean = false,
    @Column(nullable = false)
    var emailComplaint: Boolean = false,
    @Column
    var authCode: String? = null,
    @Column(nullable = false)
    var loginAttempts: Int = 0,
    @OneToMany(mappedBy = "user", fetch = LAZY)
    @MapKeyJoinColumn(name = "address_type_id")
    var addresses: MutableMap<AddressTypeEntity, UserAddressEntity> = mutableMapOf()
) : AbstractEntityDelete(), Auditable {
    @PostPersist
    override fun postPersist() {
        DomainEventPublisher.publishEvent(this.toAuditEvent(CREATE))
    }

    @PostUpdate
    override fun postUpdate() {
        DomainEventPublisher.publishEvent(this.toAuditEvent(UPDATE))
    }

    @PostRemove
    override fun postRemove() {
        DomainEventPublisher.publishEvent(this.toAuditEvent(DELETE))
    }
}
