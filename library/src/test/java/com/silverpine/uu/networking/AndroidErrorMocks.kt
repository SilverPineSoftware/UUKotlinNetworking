package com.silverpine.uu.networking

import android.os.Bundle
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.Answers
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.net.URL

/**
 * Mocks Android metadata storage while exercising real network error creation.
 * Construction mocks are scoped to the test thread and closed after each test.
 * Error creation in these tests happens on that thread, after IO work returns.
 */
internal class AndroidErrorMocks : BeforeEachCallback, AfterEachCallback
{
    override fun beforeEach(context: ExtensionContext)
    {
        val bundles = Mockito.mockConstruction(Bundle::class.java, { _ ->
            val values = mutableMapOf<String, Any?>()
            Mockito.withSettings().defaultAnswer { invocation ->
                when (invocation.method.name)
                {
                    "putString", "putInt" -> {
                        values[invocation.getArgument<String>(0)] = invocation.getArgument<Any?>(1)
                        null
                    }
                    "getString" -> values[invocation.getArgument<String>(0)] as? String
                        ?: invocation.arguments.getOrNull(1)
                    "getInt" -> values[invocation.getArgument<String>(0)] as? Int
                        ?: invocation.arguments.getOrNull(1) ?: 0
                    else -> Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        })
        context.getStore(namespace).put("bundles", bundles)
    }

    override fun afterEach(context: ExtensionContext)
    {
        (context.getStore(namespace).remove("bundles") as? MockedConstruction<*>)?.close()
    }

    private val namespace = ExtensionContext.Namespace.create(AndroidErrorMocks::class.java)
}

/** URL composition is tested separately; error/handler tests only need a fixed URL. */
internal fun mockRequestUrl(request: UUHttpRequest): UUHttpRequest =
    spy(request).also { doReturn(URL(request.url)).whenever(it).toURL }
