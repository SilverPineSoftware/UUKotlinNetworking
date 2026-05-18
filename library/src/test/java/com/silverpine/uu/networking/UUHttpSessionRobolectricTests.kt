package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.networking.handlers.UUBaseResponseHandler
import com.silverpine.uu.test.UUAssert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * [UUHttpSession.executeRequest] paths that require [UUNetworkError.makeError] / [UUNetworkError.fromException]
 * (Android [android.os.Bundle]).
 */
@ExtendWith(RobolectricExtension::class)
class UUHttpSessionRobolectricTests
{
    private val requestUrl = "https://api.example.com/test"

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
        assertEquals(expectedCode.errorDescription, error.errorDescription)
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

    private class StubHttpSession(
        private val openConnectionResult: UUResult<HttpURLConnection, UUError>,
    ) : UUHttpSession()
    {
        override suspend fun openConnection(request: UUHttpRequest): UUResult<HttpURLConnection, UUError> =
            openConnectionResult
    }

    @Test
    fun returnsSerializeFailureWhenBodyEncodeReturnsNull() = runBlocking {
        val session = StubHttpSession(UUResult.success(mockConnection()))
        val request = onlineRequest {
            method = UUHttpMethod.POST
            body = object : UUHttpBody("application/json")
            {
                override fun encode(): ByteArray? = null
            }
        }

        val response = session.executeRequest(request)

        assertFailedAt(
            response,
            UUNetworkErrorCode.SERIALIZE_FAILURE,
            UUHttpRequest.State.PrepareToSend,
        )
    }

    @Test
    fun returnsUndefinedWhenResponseCodeThrows() = runBlocking {
        val connection = mockConnection {
            whenever(responseCode).doThrow(RuntimeException("response code failed"))
        }
        val session = StubHttpSession(UUResult.success(connection))
        val request = onlineRequest()

        val response = session.executeRequest(request)

        assertFailedAt(
            response,
            UUNetworkErrorCode.UNDEFINED,
            UUHttpRequest.State.PrepareToReceive,
        )
        assertNotNull(UUAssert.unwrap(response.error).exception)
    }

    @Test
    fun returnsUndefinedWhenWriteBodyFails() = runBlocking {
        val connection = mockConnection {
            whenever(outputStream).doThrow(IOException("write failed"))
        }
        val session = StubHttpSession(UUResult.success(connection))
        val request = onlineRequest {
            method = UUHttpMethod.POST
            body = UUHttpBody("text/plain", "payload".toByteArray())
        }

        val response = session.executeRequest(request)

        assertFailedAt(
            response,
            UUNetworkErrorCode.UNDEFINED,
            UUHttpRequest.State.WriteRequest,
        )
    }

    @Test
    fun mapsHandlerExceptionToHandleResponseException() = runBlocking {
        val connection = mockConnection()
        val session = StubHttpSession(UUResult.success(connection))
        val failingHandler = object : UUBaseResponseHandler()
        {
            override suspend fun handleResponse(
                request: UUHttpRequest,
                urlConnection: HttpURLConnection,
            ): UUHttpResponse
            {
                throw RuntimeException("handler failed")
            }
        }
        val request = onlineRequest {
            responseHandler = failingHandler
        }

        val response = session.executeRequest(request)

        assertFailedAt(
            response,
            UUNetworkErrorCode.HANDLE_RESPONSE_EXCEPTION,
            UUHttpRequest.State.Complete,
        )
        assertEquals(
            RuntimeException::class.java,
            UUAssert.unwrap(response.error).exception?.javaClass,
        )
    }

    @Test
    fun openConnection_returnsOpenConnectionFailureWhenConnectionIsNotHttp() = runBlocking {
        val session = UUHttpSession()
        val request = onlineRequest(url = "file:///tmp/uu-http-session-robolectric-test")

        val result = session.openConnection(request)
        val error = UUAssert.unwrap(result.errorOrNull())

        assertEquals(UUNetworkErrorCode.OPEN_CONNECTION_FAILURE, error.uuNetworkErrorCode())
        assertEquals(UUNetworkErrorCode.OPEN_CONNECTION_FAILURE.errorDescription, error.errorDescription)
    }
}
