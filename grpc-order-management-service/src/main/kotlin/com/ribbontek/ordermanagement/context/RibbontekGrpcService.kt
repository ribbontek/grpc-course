package com.ribbontek.ordermanagement.context

import net.devh.boot.grpc.server.service.GrpcService
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@GrpcService
@NoArg
annotation class RibbontekGrpcService
