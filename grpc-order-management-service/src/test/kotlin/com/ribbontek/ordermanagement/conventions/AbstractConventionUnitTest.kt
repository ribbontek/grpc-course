package com.ribbontek.ordermanagement.conventions

import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.shared.util.logger
import org.reflections.Reflections

abstract class AbstractConventionUnitTest {
    protected val log = logger()
    protected val services: Set<Class<*>> = Reflections("com.ribbontek").getTypesAnnotatedWith(RibbontekGrpcService::class.java)
}
