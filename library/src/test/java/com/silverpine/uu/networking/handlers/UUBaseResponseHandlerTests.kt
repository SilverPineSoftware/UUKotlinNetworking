package com.silverpine.uu.networking.handlers

import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUNetworkErrorCode
import com.silverpine.uu.networking.parsers.UUBinaryStreamParser
import com.silverpine.uu.networking.parsers.uuHttpStreamParser
import com.silverpine.uu.networking.uuNetworkErrorCode
import com.silverpine.uu.networking.uuNetworkStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.IOException
import java.net.HttpURLConnection

@ExtendWith(RobolectricExtension::class)
class UUBaseResponseHandlerTests
{
    private lateinit var handler: UUBaseResponseHandler
    private lateinit var request: com.silverpine.uu.networking.UUHttpRequest

    @BeforeEach
    fun setUp()
    {
        handler = UUBaseResponseHandler()
        request = HandlerTestSupport.request()
    }

    @Nested
    inner class SuccessfulResponses
    {
        @Test
        fun parsesBodyWithSuccessParser()
        {
            runBlocking {
                val body = "payload".toByteArray()
                val connection = HandlerTestSupport.mockConnection(statusCode = 200, body = body)

                val response = handler.handleResponse(request, connection)

                assertNull(response.error)
                assertNotNull(response.parsedResponse)
                assertArrayEquals(body, response.parsedResponse as ByteArray)
            }
        }

        @Test
        fun decompressesGzipEncodedBody()
        {
            runBlocking {
                val plain = """{"id":"gzip"}""".toByteArray()
                val gzipped = HandlerTestSupport.gzipBody(plain)
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 200,
                    body = gzipped,
                    contentEncoding = "gzip",
                )

                val response = handler.handleResponse(request, connection)

                assertNull(response.error)
                assertArrayEquals(plain, response.parsedResponse as ByteArray)
            }
        }
    }

    @Nested
    inner class ErrorResponses
    {
        @Test
        fun maps404ToHttpFailureNetworkError()
        {
            runBlocking {
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 404,
                    body = "not found".toByteArray(),
                )

                val response = handler.handleResponse(request, connection)

                assertEquals(UUNetworkErrorCode.HTTP_FAILURE, response.error?.uuNetworkErrorCode())
                assertArrayEquals("not found".toByteArray(), response.parsedResponse as ByteArray)
            }
        }

        @Test
        fun maps401ToAuthorizationNeeded()
        {
            runBlocking {
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 401,
                    body = """{"message":"unauthorized"}""".toByteArray(),
                )

                val response = handler.handleResponse(request, connection)

                assertEquals(UUNetworkErrorCode.AUTHORIZATION_NEEDED, response.error?.uuNetworkErrorCode())
                assertEquals(401, response.error?.uuNetworkStatusCode())
            }
        }

        @Test
        fun usesErrorParserForNonSuccessStatus()
        {
            runBlocking {
                var usedErrorParser = false
                val customHandler = object : UUBaseResponseHandler()
                {
                    override val successParser = uuHttpStreamParser { _, _ -> "success" }
                    override val errorParser = uuHttpStreamParser { _, _ ->
                        usedErrorParser = true
                        "error-body"
                    }
                }
                val connection = HandlerTestSupport.mockConnection(statusCode = 500, body = "err".toByteArray())

                val response = customHandler.handleResponse(request, connection)

                assertEquals(true, usedErrorParser)
                assertEquals("error-body", response.parsedResponse)
                assertNotNull(response.error)
            }
        }
    }

    @Nested
    inner class ParserResults
    {
        @Test
        fun honorsUUErrorReturnedFromParser()
        {
            runBlocking {
                val injectedError = UUError(99, "TestDomain")
                val customHandler = object : UUBaseResponseHandler()
                {
                    override val successParser = uuHttpStreamParser { _, _ -> injectedError }
                    override val errorParser = UUBinaryStreamParser()
                }
                val connection = HandlerTestSupport.mockConnection(statusCode = 200, body = "{}".toByteArray())

                val response = customHandler.handleResponse(request, connection)

                assertSame(injectedError, response.error)
                assertNull(response.parsedResponse)
            }
        }
    }

    @Nested
    inner class ExceptionHandling
    {
        @Test
        fun mapsReadExceptionToReadFailed()
        {
            runBlocking {
                val connection = mock<HttpURLConnection>()
                whenever(connection.responseCode).thenReturn(200)
                whenever(connection.contentEncoding).thenReturn(null)
                whenever(connection.inputStream).thenThrow(IOException("stream failed"))

                val response = handler.handleResponse(request, connection)

                assertEquals(UUNetworkErrorCode.READ_FAILED, response.error?.uuNetworkErrorCode())
                assertNotNull(response.error?.exception)
            }
        }
    }

    @Nested
    inner class DefaultParsers
    {
        @Test
        fun usesBinaryStreamParserForSuccessAndError()
        {
            runBlocking {
                assertInstanceOf(UUBinaryStreamParser::class.java, handler.successParser)
                assertInstanceOf(UUBinaryStreamParser::class.java, handler.errorParser)
            }
        }
    }
}
