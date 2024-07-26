package com.ribbontek.ordermanagement.security

import com.ribbontek.ordermanagement.context.RibbontekGrpcService
import com.ribbontek.ordermanagement.service.security.SecurityService
import com.ribbontek.ordermanagement.util.newInstanceOf
import com.ribbontek.shared.util.logger
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerInterceptor
import io.grpc.ServerServiceDefinition
import io.grpc.kotlin.CoroutineContextServerInterceptor
import kotlinx.coroutines.Dispatchers
import net.devh.boot.grpc.common.util.InterceptorOrder
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.reflections.Reflections
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.functions

@Configuration
class GrpcSecurityInterceptorConfig(
    private val securityService: SecurityService
) {
    private val log = logger()
    private val methodPermissionsMap: Map<String, RequiresOAuthPermission?> = run {
        val classes = getRibbontekGrpcServiceClassesInPackage()
        val clazzMethodToPermissionMap = classes.toClassMethodPermissionMap()
        classes.mapNotNull { clazz ->
            val methodToPermissionMap = clazzMethodToPermissionMap[clazz]
            // requires NoArg to instantiate a newInstanceOf RibbontekGrpcService
            val bindableService = clazz.kotlin.functions.find { it.name == "bindService" }?.call(clazz.newInstanceOf()) as? ServerServiceDefinition
            bindableService?.methods?.map { it.methodDescriptor.fullMethodName to methodToPermissionMap?.get(it.methodDescriptor.bareMethodName) }
        }.flatten().toMap()
    }

    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_SECURITY_AUTHENTICATION)
    fun grpcSecurityInterceptor(): ServerInterceptor {
        return object : CoroutineContextServerInterceptor() {
            override fun coroutineContext(
                call: ServerCall<*, *>,
                headers: Metadata
            ): CoroutineContext {
                val requiresOAuthPermission = methodPermissionsMap[call.methodDescriptor.fullMethodName]
                if (requiresOAuthPermission?.let { arrayOf(*it.value).isNotEmpty() } == true) {
                    val methodName = call.methodDescriptor.bareMethodName ?: "unknown"
                    securityService.authenticate(methodName, headers, arrayOf(*requiresOAuthPermission.value))
                    log.info("Authenticated User Token")
                }
                return Dispatchers.Default + SecurityCoroutineContext()
            }
        }
    }

    private fun getRibbontekGrpcServiceClassesInPackage(): Set<Class<*>> =
        Reflections("com.ribbontek.ordermanagement.grpc").getTypesAnnotatedWith(RibbontekGrpcService::class.java)

    private fun Set<Class<*>>.toClassMethodPermissionMap(): Map<Class<*>, Map<String, RequiresOAuthPermission>> =
        associate { clazz -> clazz to clazz.methods.associate { it.name.withCapitalization() to it.getAnnotation(RequiresOAuthPermission::class.java) } }

    private fun String.withCapitalization() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
