package com.ribbontek.ordermanagement.grpc

import com.google.protobuf.Empty
import com.ribbontek.grpccourse.UnsubscribeServiceGrpcKt.UnsubscribeServiceCoroutineImplBase
import com.ribbontek.grpccourse.UnsubscribeUserCommand
import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.mapping.toUnsubscribeUserCommandModel
import com.ribbontek.ordermanagement.service.unsubscribe.UnsubscribeUserService

@RibbontekGrpcService
class UnsubscribeGrpcService(
    private val unsubscribeUserService: UnsubscribeUserService
) : UnsubscribeServiceCoroutineImplBase() {

    override suspend fun unsubscribe(request: UnsubscribeUserCommand): Empty {
        unsubscribeUserService.unsubscribeUser(request.toUnsubscribeUserCommandModel())
        return Empty.getDefaultInstance()
    }
}
