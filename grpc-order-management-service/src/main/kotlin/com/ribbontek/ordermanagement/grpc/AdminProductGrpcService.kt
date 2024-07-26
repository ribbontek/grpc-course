package com.ribbontek.ordermanagement.grpc

import com.ribbontek.grpccourse.PagingRequest
import com.ribbontek.grpccourse.PagingResponse
import com.ribbontek.grpccourse.admin.AdminCategory
import com.ribbontek.grpccourse.admin.AdminDiscount
import com.ribbontek.grpccourse.admin.AdminProduct
import com.ribbontek.grpccourse.admin.AdminProductServiceGrpcKt.AdminProductServiceCoroutineImplBase
import com.ribbontek.grpccourse.admin.UpsertCategoryCommand
import com.ribbontek.grpccourse.admin.UpsertDiscountCommand
import com.ribbontek.grpccourse.admin.UpsertProductCommand
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toAdminCategoryRequestModel
import com.ribbontek.ordermanagement.mapping.toAdminDiscountRequestModel
import com.ribbontek.ordermanagement.mapping.toAdminProductRequestModel
import com.ribbontek.ordermanagement.mapping.toUpsertCategoryCommandModel
import com.ribbontek.ordermanagement.mapping.toUpsertDiscountCommandModel
import com.ribbontek.ordermanagement.mapping.toUpsertProductCommandModel
import com.ribbontek.ordermanagement.security.RequiresOAuthPermission
import com.ribbontek.ordermanagement.service.admin.AdminProductUpsertService
import com.ribbontek.ordermanagement.service.admin.AdminProductViewService

@RibbontekGrpcService
class AdminProductGrpcService(
    private val adminProductUpsertService: AdminProductUpsertService,
    private val adminProductViewService: AdminProductViewService
) : AdminProductServiceCoroutineImplBase() {

    @RequiresOAuthPermission("admin:view")
    override suspend fun getAdminPagedProducts(request: PagingRequest): PagingResponse {
        return adminProductViewService.getPagedProducts(request.toAdminProductRequestModel())
    }

    @RequiresOAuthPermission("admin:view")
    override suspend fun getAdminPagedDiscounts(request: PagingRequest): PagingResponse {
        return adminProductViewService.getPagedDiscounts(request.toAdminDiscountRequestModel())
    }

    @RequiresOAuthPermission("admin:view")
    override suspend fun getAdminPagedCategories(request: PagingRequest): PagingResponse {
        return adminProductViewService.getPagedCategories(request.toAdminCategoryRequestModel())
    }

    @RequiresOAuthPermission("admin:create")
    override suspend fun upsertDiscount(request: UpsertDiscountCommand): AdminDiscount {
        return adminProductUpsertService.upsertDiscount(request.toUpsertDiscountCommandModel())
    }

    @RequiresOAuthPermission("admin:create")
    override suspend fun upsertCategory(request: UpsertCategoryCommand): AdminCategory {
        return adminProductUpsertService.upsertCategory(request.toUpsertCategoryCommandModel())
    }

    @RequiresOAuthPermission("admin:create")
    override suspend fun upsertProduct(request: UpsertProductCommand): AdminProduct {
        return adminProductUpsertService.upsertProduct(request.toUpsertProductCommandModel())
    }
}
