package com.ribbontek.ordermanagement.grpc

import com.google.protobuf.Empty
import com.ribbontek.grpccourse.admin.AdminAsset
import com.ribbontek.grpccourse.admin.AdminAssetServiceGrpcKt.AdminAssetServiceCoroutineImplBase
import com.ribbontek.grpccourse.admin.DeleteAssetCommand
import com.ribbontek.grpccourse.admin.GetAssetCommand
import com.ribbontek.grpccourse.admin.UploadAssetCommand
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toAdminAsset
import com.ribbontek.ordermanagement.mapping.toDeleteAssetCommandModel
import com.ribbontek.ordermanagement.mapping.toGetAssetCommandModel
import com.ribbontek.ordermanagement.mapping.toUploadAssetCommandModel
import com.ribbontek.ordermanagement.security.RequiresOAuthPermission
import com.ribbontek.ordermanagement.service.admin.AdminAssetService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

@RibbontekGrpcService
class AdminAssetGrpcService(
    private val adminAssetService: AdminAssetService
) : AdminAssetServiceCoroutineImplBase() {

    @RequiresOAuthPermission("admin:create")
    override suspend fun uploadAsset(requests: Flow<UploadAssetCommand>): Empty {
        adminAssetService.uploadAssets(requests.map { it.toUploadAssetCommandModel() }.toList())
        return Empty.getDefaultInstance()
    }

    @RequiresOAuthPermission("admin:view")
    override fun getAsset(request: GetAssetCommand): Flow<AdminAsset> {
        return adminAssetService.getAssets(request.toGetAssetCommandModel()).map { it.toAdminAsset() }.asFlow()
    }

    @RequiresOAuthPermission("admin:delete")
    override suspend fun deleteAsset(request: DeleteAssetCommand): Empty {
        adminAssetService.deleteAssets(request.toDeleteAssetCommandModel())
        return Empty.getDefaultInstance()
    }
}
