package com.ribbontek.ordermanagement.repository.asset

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetRepository : JpaRepository<AssetEntity, Long> {
    fun findAllByNameAndAssetReferenceAndAssetType(
        name: String,
        assetReference: String,
        assetType: AssetTypeEnum
    ): List<AssetEntity>
}
