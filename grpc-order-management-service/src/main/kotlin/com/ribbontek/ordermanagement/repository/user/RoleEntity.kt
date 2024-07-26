package com.ribbontek.ordermanagement.repository.user

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Cacheable
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType

enum class RoleType {
    STANDARD,
    ADMIN
}

@Entity
@Table(name = "role")
@AttributeOverride(name = "id", column = Column(name = "role_id"))
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class RoleEntity(
    @Column(nullable = false, columnDefinition = "role_type_enum")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var roleType: RoleType,
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val user: UserEntity? = null,
    @ManyToMany(cascade = [CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH], fetch = FetchType.EAGER)
    @JoinTable(
        schema = "grpccourse",
        name = "role_to_policy",
        joinColumns = [JoinColumn(name = "role_id", referencedColumnName = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "policy_id", referencedColumnName = "policy_id")]
    )
    var policies: MutableSet<PolicyEntity> = mutableSetOf()
) : AbstractEntity()
