package com.ribbontek.ordermanagement.generator

import com.ribbontek.ordermanagement.factory.entity.RoleEntityFactory
import com.ribbontek.ordermanagement.factory.entity.UserAddressFactory
import com.ribbontek.ordermanagement.factory.entity.UserEntityFactory
import com.ribbontek.ordermanagement.repository.user.AddressTypeEnum
import com.ribbontek.ordermanagement.repository.user.AddressTypeRepository
import com.ribbontek.ordermanagement.repository.user.PolicyEntity
import com.ribbontek.ordermanagement.repository.user.RoleRepository
import com.ribbontek.ordermanagement.repository.user.RoleType.ADMIN
import com.ribbontek.ordermanagement.repository.user.RoleType.STANDARD
import com.ribbontek.ordermanagement.repository.user.UserAddressEntity
import com.ribbontek.ordermanagement.repository.user.UserAddressRepository
import com.ribbontek.ordermanagement.repository.user.UserEntity
import com.ribbontek.ordermanagement.repository.user.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

interface UserGenerator {
    fun generateAdmin(): Pair<UserEntity, List<UserAddressEntity>>

    fun generateStandard(): Pair<UserEntity, List<UserAddressEntity>>
}

@Component
class UserGeneratorImpl(
    private val userRepository: UserRepository,
    private val addressTypeRepository: AddressTypeRepository,
    private val userAddressRepository: UserAddressRepository,
    private val roleRepository: RoleRepository
) : UserGenerator {
    @Transactional
    override fun generateAdmin(): Pair<UserEntity, List<UserAddressEntity>> {
        val userEntity = userRepository.save(UserEntityFactory.create())
        roleRepository.save(
            RoleEntityFactory.create(
                userEntity = userEntity,
                roleType = ADMIN,
                policies =
                listOf(
                    PolicyEntity(permission = "shopping:*"),
                    PolicyEntity(permission = "product:*"),
                    PolicyEntity(permission = "order:*"),
                    PolicyEntity(permission = "account:*"),
                    PolicyEntity(permission = "admin:*")
                )
            )
        )
        val userAddresses =
            userAddressRepository.saveAll(
                listOf(
                    UserAddressFactory.create(
                        userEntity = userEntity,
                        addressTypeEntity = addressTypeRepository.findByCode(AddressTypeEnum.RESIDENTIAL.code)!!
                    ),
                    UserAddressFactory.create(
                        userEntity = userEntity,
                        addressTypeEntity = addressTypeRepository.findByCode(AddressTypeEnum.POSTAL.code)!!
                    )
                )
            )
        return userEntity to userAddresses
    }

    @Transactional
    override fun generateStandard(): Pair<UserEntity, List<UserAddressEntity>> {
        val userEntity = userRepository.save(UserEntityFactory.create())
        roleRepository.save(
            RoleEntityFactory.create(
                userEntity = userEntity,
                roleType = STANDARD,
                policies =
                listOf(
                    PolicyEntity(permission = "shopping:*"),
                    PolicyEntity(permission = "product:*"),
                    PolicyEntity(permission = "order:*"),
                    PolicyEntity(permission = "account:*")
                )
            )
        )
        val userAddresses =
            userAddressRepository.saveAll(
                listOf(
                    UserAddressFactory.create(
                        userEntity = userEntity,
                        addressTypeEntity = addressTypeRepository.findByCode(AddressTypeEnum.RESIDENTIAL.code)!!
                    ),
                    UserAddressFactory.create(
                        userEntity = userEntity,
                        addressTypeEntity = addressTypeRepository.findByCode(AddressTypeEnum.POSTAL.code)!!
                    )
                )
            )
        return userEntity to userAddresses
    }
}
