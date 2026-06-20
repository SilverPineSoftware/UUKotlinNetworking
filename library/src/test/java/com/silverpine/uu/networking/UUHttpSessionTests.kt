package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.networking.connectivity.UUConnectivityProvider
import com.silverpine.uu.test.UUAssert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * JVM unit tests for [UUHttpSession.execute] using stubbed connection/handler results.
 *
 * Paths that require real [UUNetworkError] / [android.os.Bundle] are in [UUHttpSessionRobolectricTests].
 */
class UUHttpSessionTests
{
    private val requestUrl = "https://api.example.com/test"

    private fun networkError(code: UUNetworkErrorCode): UUError =
        UUError(code.value, UUNetworkError.DOMAIN)

    private fun onlineRequest(
        url: String = requestUrl,
        configure: UUHttpRequest.() -> Unit = {},
    ): UUHttpRequest =
        UUHttpRequest(url = url).apply {
            connectivityProvider = mock {
                on { checkConnection() } doReturn null
            }
            configure()
        }

    private fun assertFailedAt(
        response: UUHttpResponse,
        expectedCode: UUNetworkErrorCode,
        expectedState: UUHttpRequest.State,
    )
    {
        val error = UUAssert.unwrap(response.error)
        assertEquals(expectedCode, error.uuNetworkErrorCode())
        assertEquals(expectedState, response.request.state)
        assertNull(response.parsedResponse)
    }

    private fun mockConnection(configure: HttpURLConnection.() -> Unit = {}): HttpURLConnection
    {
        val connection = mock<HttpURLConnection>()
        whenever(connection.responseCode).doReturn(200)
        whenever(connection.contentType).doReturn("application/json")
        whenever(connection.contentEncoding).doReturn("")
        whenever(connection.headerFields).doReturn(emptyMap())
        whenever(connection.inputStream).doReturn(ByteArrayInputStream("{}".toByteArray()))
        whenever(connection.url).doReturn(URL(requestUrl))
        whenever(connection.requestMethod).doReturn("GET")
        connection.configure()
        return connection
    }

    /**
     * Supplies [openConnection] / [handleResponse] results for [execute] tests.
     */
    private inner open class StubHttpSession(
        private val openConnectionResult: UUResult<HttpURLConnection, UUError>? = null,
        private val handleResponseResult: UUHttpResponse? = null,
    ) : UUHttpSession()
    {
        override suspend fun openConnection(request: UUHttpRequest): UUResult<HttpURLConnection, UUError>
        {
            return openConnectionResult
                ?: UUResult.failure(networkError(UUNetworkErrorCode.OPEN_CONNECTION_FAILURE))
        }

        override suspend fun handleResponse(
            request: UUHttpRequest,
            urlConnection: HttpURLConnection,
        ): UUHttpResponse
        {
            return handleResponseResult
                ?: UUHttpResponse(request = request, parsedResponse = "ok")
        }
    }

    @Nested
    inner class CheckConnectionErrors
    {
        @Test
        fun returnsConnectivityErrorWithoutOpeningConnection() = runBlocking {
            val injectedError = UUError(-5757, "UnitTestErrorDomain")
            val connectivity = mock<UUConnectivityProvider> {
                on { checkConnection() } doReturn injectedError
            }

            val session = UUHttpSession()
            val request = UUHttpRequest(url = requestUrl).apply {
                connectivityProvider = connectivity
            }

            val response = session.execute(request)

            val error = UUAssert.unwrap(response.error)
            assertEquals(injectedError.code, error.code)
            assertEquals(injectedError.domain, error.domain)
            assertEquals(UUHttpRequest.State.CheckConnection, response.request.state)
        }

        @Test
        fun skipsCheckWhenConnectivityProviderIsNull() = runBlocking {
            val connection = mockConnection()
            val session = StubHttpSession(UUResult.success(connection))
            val request = onlineRequest {
                connectivityProvider = null
            }

            val response = session.execute(request)

            assertNotNull(response)
            assertEquals(UUHttpRequest.State.Complete, response.request.state)
        }
    }

    @Nested
    inner class OpenConnectionErrors
    {
        @Test
        fun returnsErrorWhenOpenConnectionFails() = runBlocking {
            val session = StubHttpSession(
                UUResult.failure(networkError(UUNetworkErrorCode.OPEN_CONNECTION_FAILURE)),
            )
            val response = session.execute(onlineRequest())

            assertFailedAt(
                response,
                UUNetworkErrorCode.OPEN_CONNECTION_FAILURE,
                UUHttpRequest.State.OpenConnection,
            )
        }

        @Test
        fun propagatesTimeoutFromOpenConnection() = runBlocking {
            val session = StubHttpSession(
                UUResult.failure(networkError(UUNetworkErrorCode.TIMEOUT)),
            )
            val response = session.execute(onlineRequest())

            assertFailedAt(response, UUNetworkErrorCode.TIMEOUT, UUHttpRequest.State.OpenConnection)
        }
    }

    @Nested
    inner class PrepareToSendErrors
    {
        @Test
        fun returnsSerializeFailureWhenPrepareToSendFails() = runBlocking {
            val session = StubHttpSession(UUResult.success(mockConnection()))
            val request = onlineRequest {
                method = UUHttpMethod.POST
                body = object : UUHttpBody("application/json")
                {
                    override fun prepareToSend(): UUResult<Pair<ByteArray, UUHttpHeaders>?, UUError> =
                        UUResult.failure(networkError(UUNetworkErrorCode.SERIALIZE_FAILURE))
                }
            }

            val response = session.execute(request)

            assertFailedAt(
                response,
                UUNetworkErrorCode.SERIALIZE_FAILURE,
                UUHttpRequest.State.PrepareToSend,
            )
        }
    }

    @Nested
    inner class HandleResponseErrors
    {
        @Test
        fun returnsErrorReturnedFromHandleResponse() = runBlocking {
            val connection = mockConnection()
            val request = onlineRequest()
            val session = object : StubHttpSession(UUResult.success(connection))
            {
                override suspend fun handleResponse(
                    request: UUHttpRequest,
                    urlConnection: HttpURLConnection,
                ): UUHttpResponse
                {
                    return UUHttpResponse(
                        request = request,
                        error = networkError(UUNetworkErrorCode.HANDLE_RESPONSE_EXCEPTION),
                    )
                }
            }

            val response = session.execute(request)

            assertFailedAt(
                response,
                UUNetworkErrorCode.HANDLE_RESPONSE_EXCEPTION,
                UUHttpRequest.State.Complete,
            )
        }
    }

    @Nested
    inner class SuccessfulCompletion
    {
        @Test
        fun completesWhenHandleResponseSucceeds() = runBlocking {
            val connection = mockConnection()
            val session = StubHttpSession(UUResult.success(connection))
            val request = onlineRequest()

            val response = session.execute(request)

            assertNull(response.error)
            assertEquals("ok", response.parsedResponse)
            assertEquals(UUHttpRequest.State.Complete, response.request.state)
        }
    }
}
