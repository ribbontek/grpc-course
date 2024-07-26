package com.ribbontek.ordermanagement.mapping

import com.google.protobuf.kotlin.toByteString
import com.ribbontek.grpccourse.admin.AdminAsset
import com.ribbontek.grpccourse.admin.AssetType
import com.ribbontek.grpccourse.admin.AssetType.GLOBAL
import com.ribbontek.grpccourse.admin.AssetType.PRODUCT
import com.ribbontek.grpccourse.admin.AssetType.UNRECOGNIZED
import com.ribbontek.grpccourse.admin.DeleteAssetCommand
import com.ribbontek.grpccourse.admin.GetAssetCommand
import com.ribbontek.grpccourse.admin.UploadAssetCommand
import com.ribbontek.grpccourse.admin.adminAsset
import com.ribbontek.ordermanagement.grpc.model.AdminAssetModel
import com.ribbontek.ordermanagement.grpc.model.DeleteAssetCommandModel
import com.ribbontek.ordermanagement.grpc.model.GetAssetCommandModel
import com.ribbontek.ordermanagement.grpc.model.UploadAssetCommandModel
import com.ribbontek.ordermanagement.repository.asset.AssetEntity
import com.ribbontek.ordermanagement.repository.asset.AssetTypeEnum
import jakarta.validation.ConstraintViolationException

fun GetAssetCommand.toGetAssetCommandModel(): GetAssetCommandModel {
    val source = this
    return GetAssetCommandModel(
        name = source.name,
        assetType = source.assetType.toAssetTypeEnum(),
        assetReference = source.assetReference
    )
}

fun DeleteAssetCommand.toDeleteAssetCommandModel(): DeleteAssetCommandModel {
    val source = this
    return DeleteAssetCommandModel(
        name = source.name,
        assetType = source.assetType.toAssetTypeEnum(),
        assetReference = source.assetReference
    )
}

fun UploadAssetCommand.toUploadAssetCommandModel(): UploadAssetCommandModel {
    val source = this
    return UploadAssetCommandModel(
        name = source.name,
        multipart = source.multipart,
        partNumber = if (source.hasPartNumber()) source.partNumber else null,
        totalNumber = if (source.hasTotalNumber()) source.totalNumber else null,
        content = source.content.toByteArray(),
        assetType = source.assetType.toAssetTypeEnum(),
        assetReference = source.assetReference
    )
}

fun AssetType.toAssetTypeEnum(): AssetTypeEnum {
    return when (this) {
        GLOBAL -> AssetTypeEnum.GLOBAL
        PRODUCT -> AssetTypeEnum.PRODUCT
        UNRECOGNIZED -> throw ConstraintViolationException("AssetType must be provided", emptySet())
    }
}

fun UploadAssetCommandModel.toAssetEntity(): AssetEntity {
    val source = this
    return AssetEntity(
        name = source.name,
        multipart = source.multipart,
        partNumber = source.partNumber ?: 1,
        totalNumber = source.totalNumber ?: 1,
        assetType = source.assetType,
        assetReference = source.assetReference
    )
}

fun AssetEntity.toAdminAsset(content: ByteArray): AdminAssetModel {
    val source = this
    return AdminAssetModel(
        name = source.name,
        multipart = source.multipart,
        partNumber = source.partNumber,
        totalNumber = source.totalNumber,
        content = content.toByteString(),
        assetType = source.assetType,
        assetReference = source.assetReference
    )
}

fun AdminAssetModel.toAdminAsset(): AdminAsset {
    val source = this
    return adminAsset {
        this.name = source.name
        this.multipart = source.multipart
        this.partNumber = source.partNumber
        this.totalNumber = source.totalNumber
        this.content = source.content
        this.assetType = source.assetType.toAssetType()
        this.assetReference = source.assetReference
    }
}

fun AssetTypeEnum.toAssetType(): AssetType {
    return when (this) {
        AssetTypeEnum.GLOBAL -> GLOBAL
        AssetTypeEnum.PRODUCT -> PRODUCT
    }
}
