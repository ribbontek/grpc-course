package com.ribbontek.ordermanagement.repository.user

import com.fasterxml.jackson.annotation.JsonIgnore
import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_address")
@AttributeOverride(name = "id", column = Column(name = "user_address_id"))
class UserAddressEntity(
    @JsonIgnore
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    val user: UserEntity? = null,
    @JsonIgnore
    @JoinColumn(name = "address_type_id", referencedColumnName = "address_type_id", nullable = false)
    @ManyToOne(optional = false)
    var addressType: AddressTypeEntity,
    @Column(nullable = false, length = 255)
    var line: String,
    @Column(nullable = false, length = 100)
    var suburb: String,
    @Column(nullable = true, length = 50)
    var state: String? = null,
    @Column(nullable = true, length = 15)
    var postcode: String? = null,
    @Column(nullable = false, length = 100)
    var country: String
) : AbstractEntity()
