package com.bluedragonmc.api

import io.grpc.MethodDescriptor
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.fail

class RpcRetryPolicyTest {

    @Test
    fun `every retryable method exists in the generated service descriptors`() {
        val missing = RpcRetryPolicy.retryableMethods.flatMap { (service, methods) ->
            val declared = declaredMethods(service)
            methods.filterNot { it in declared }.map { "$service/$it" }
        }

        if (missing.isNotEmpty()) {
            fail("RpcRetryPolicy refers to methods that no longer exist: ${missing.joinToString()}")
        }
    }

    private fun declaredMethods(service: String): Set<String> {
        val grpcClass = try {
            Class.forName("com.bluedragonmc.api.grpc.${service}Grpc")
        } catch (_: ClassNotFoundException) {
            fail("No generated gRPC class for service '$service'")
        }

        return grpcClass.declaredMethods
            .asSequence()
            .filter { Modifier.isStatic(it.modifiers) && it.name.endsWith("Method") && it.parameterCount == 0 }
            .mapNotNull { it.invoke(null) as? MethodDescriptor<*, *> }
            .mapNotNull { it.bareMethodName }
            .toSet()
    }
}
