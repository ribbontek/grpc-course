package com.ribbontek.ordermanagement.factory

import com.google.protobuf.ByteString
import com.ribbontek.grpccourse.admin.AssetType
import com.ribbontek.grpccourse.admin.UploadAssetCommand
import com.ribbontek.grpccourse.admin.uploadAssetCommand

object UploadAssetCommandFactory {

    fun create(
        name: String,
        content: ByteArray,
        assetType: AssetType,
        assetReference: String,
        multipart: Boolean? = null,
        partNumber: Int? = null,
        totalNumber: Int? = null
    ): UploadAssetCommand {
        return uploadAssetCommand {
            this.name = name
            this.multipart = multipart ?: false
            if (multipart == true) {
                this.partNumber = partNumber ?: 1
                this.totalNumber = totalNumber ?: 1
            }
            this.content = ByteString.copyFrom(content)
            this.assetType = assetType
            this.assetReference = assetReference
        }
    }
}
