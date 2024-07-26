package com.ribbontek.ordermanagement.repository.user

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

@Entity
@Table(name = "policy")
@AttributeOverride(name = "id", column = Column(name = "policy_id"))
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class PolicyEntity(
    @Column(nullable = false, length = 255)
    var permission: String
) : AbstractEntity()
