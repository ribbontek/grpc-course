package com.ribbontek.ordermanagement.conventions

import com.ribbontek.ordermanagement.security.RequiresOAuthPermission
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthorisationConventionUnitTest : AbstractConventionUnitTest() {

    @Test
    fun `convention test - RibbontekGrpcService classes have appropriate configured RequiresOAuthPermission on endpoint methods`() {
        services.forEach { clazz ->
            clazz.methods.filter { it.isDefault }.forEach { function ->
                log.info("Validating Class: ${clazz.simpleName}, Function ${function.name} for annotation @Rest2GrpcEndpoint")
                if (function.isAnnotationPresent(RequiresOAuthPermission::class.java)) {
                    assertTrue(
                        function.getAnnotation(RequiresOAuthPermission::class.java).value.isNotEmpty(),
                        "Expected RequiresOAuthPermission to be configured with permissions"
                    )
                }
            }
        }
    }
}
