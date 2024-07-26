package com.ribbontek.ordermanagement.repository.user

import com.ribbontek.ordermanagement.exception.NotFoundException
import com.ribbontek.ordermanagement.repository.abstracts.EntityCodeDescriptionRepository
import org.springframework.stereotype.Repository

@Repository
interface AddressTypeRepository : EntityCodeDescriptionRepository<AddressTypeEntity>

fun AddressTypeRepository.expectOneByCode(code: String): AddressTypeEntity =
    findByCode(code) ?: throw NotFoundException("Could not find address type by code $code")
