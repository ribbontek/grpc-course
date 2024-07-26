package com.ribbontek.ordermanagement.config

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerInterceptor
import io.grpc.kotlin.CoroutineContextServerInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import net.devh.boot.grpc.common.util.InterceptorOrder
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.slf4j.MDC
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import java.util.UUID
import kotlin.coroutines.CoroutineContext

@Configuration
class CorrelationIdInterceptorConfig {

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
    }

    @GrpcGlobalServerInterceptor
    @Order(InterceptorOrder.ORDER_FIRST)
    fun correlationIdInterceptor(): ServerInterceptor {
        return object : CoroutineContextServerInterceptor() {
            override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
                val correlationId = headers.get(Metadata.Key.of(CORRELATION_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER))
                    .takeIf { !it.isNullOrEmpty() } ?: UUID.randomUUID().toString()
                MDC.put(CORRELATION_ID_HEADER, correlationId)
                return Dispatchers.Default + MDCContext()
            }
        }
    }
}
