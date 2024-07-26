package com.ribbontek.ordermanagement.mapping

import com.ribbontek.grpccourse.UnsubscribeUserCommand
import com.ribbontek.ordermanagement.grpc.model.UnsubscribeUserCommandModel

fun UnsubscribeUserCommand.toUnsubscribeUserCommandModel(): UnsubscribeUserCommandModel {
    return UnsubscribeUserCommandModel(
        email = email,
        code = code
    )
}
