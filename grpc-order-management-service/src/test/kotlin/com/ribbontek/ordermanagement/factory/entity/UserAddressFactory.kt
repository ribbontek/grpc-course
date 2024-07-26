package com.ribbontek.ordermanagement.factory.entity

import com.ribbontek.ordermanagement.repository.user.AddressTypeEntity
import com.ribbontek.ordermanagement.repository.user.UserAddressEntity
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.util.FakerUtil

object UserAddressFactory {
    fun create(
        userEntity: UserEntity,
        addressTypeEntity: AddressTypeEntity
    ): UserAddressEntity {
        return UserAddressEntity(
            user = userEntity,
            addressType = addressTypeEntity,
            line = FakerUtil.addressLine(),
            suburb = FakerUtil.suburb(),
            state = FakerUtil.state(),
            postcode = FakerUtil.postcode(),
            country = FakerUtil.country()
        )
    }
}
