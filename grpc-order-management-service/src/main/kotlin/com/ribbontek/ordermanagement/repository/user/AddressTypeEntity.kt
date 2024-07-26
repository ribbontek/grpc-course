package com.ribbontek.ordermanagement.repository.user

import com.ribbontek.ordermanagement.repository.abstracts.AbstractEntityCodeDescription
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

enum class AddressTypeEnum(val code: String) {
    RESIDENTIAL("Residential"),
    POSTAL("Postal"),
    DELIVERY("Delivery"),
    BILLING("Billing");
    companion object {
        fun findByCode(code: String): AddressTypeEnum? = entries.find { it.code == code }
    }
}

@Entity(name = "AddressTypeEntity")
@Table(name = "address_type")
@AttributeOverride(name = "id", column = Column(name = "address_type_id"))
class AddressTypeEntity() : AbstractEntityCodeDescription() {
    constructor(addressTypeEnum: AddressTypeEnum) : this() {
        code = addressTypeEnum.code
    }
}
