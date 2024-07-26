package com.ribbontek.ordermanagement.grpc.model

import com.google.protobuf.ByteString
import com.ribbontek.ordermanagement.grpc.model.validator.NotBlankOrEmpty
import com.ribbontek.ordermanagement.repository.asset.AssetTypeEnum
import jakarta.validation.constraints.Size

data class UploadAssetCommandModel(
    @get:[
        NotBlankOrEmpty
        Size(min = 1, max = 255)
    ] val name: String,
    val multipart: Boolean,
    val partNumber: Int? = null,
    val totalNumber: Int? = null,
    @get:[
        Size(min = 1, max = 5 * 1024 * 1024) // 5MB
    ]
    val content: ByteArray,
    val assetType: AssetTypeEnum,
    @get:[
        NotBlankOrEmpty
        Size(min = 1, max = 255)
    ] val assetReference: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadAssetCommandModel

        if (name != other.name) return false
        if (multipart != other.multipart) return false
        if (partNumber != other.partNumber) return false
        if (totalNumber != other.totalNumber) return false
        if (!content.contentEquals(other.content)) return false
        if (assetType != other.assetType) return false
        if (assetReference != other.assetReference) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + multipart.hashCode()
        result = 31 * result + (partNumber ?: 0)
        result = 31 * result + (totalNumber ?: 0)
        result = 31 * result + content.contentHashCode()
        result = 31 * result + assetType.hashCode()
        result = 31 * result + assetReference.hashCode()
        return result
    }
}

data class GetAssetCommandModel(
    @get:[
        NotBlankOrEmpty
        Size(min = 1, max = 255)
    ] val name: String,
    val assetType: AssetTypeEnum,
    @get:[
        NotBlankOrEmpty
        Size(min = 1, max = 255)
    ] val assetReference: String
)

data class DeleteAssetCommandModel(
    @get:[
        NotBlankOrEmpty
        Size(min = 1, max = 255)
    ] val name: String,
    val assetType: AssetTypeEnum,
    @get:[
        NotBlankOrEmpty
        Size(min = 1, max = 255)
    ] val assetReference: String
)

data class AdminAssetModel(
    val name: String,
    val multipart: Boolean,
    val partNumber: Int,
    val totalNumber: Int,
    val content: ByteString,
    val assetType: AssetTypeEnum,
    val assetReference: String
)
