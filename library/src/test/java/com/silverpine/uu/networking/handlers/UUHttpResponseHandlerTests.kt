package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.parsers.UUBinaryStreamParser
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import com.silverpine.uu.networking.parsers.uuHttpStreamParser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.net.HttpURLConnection

@ExtendWith(RobolectricExtension::class)
class UUHttpResponseHandlerTests
{
    @Test
    fun implementationExposesSuccessAndErrorParsers()
    {
        runBlocking {
            val successParser = uuHttpStreamParser { _, _ -> "success" }
            val errorParser = uuHttpStreamParser { _, _ -> "error" }
            val handler = RecordingHandler(successParser, errorParser)

            assertSame(successParser, handler.successParser)
            assertSame(errorParser, handler.errorParser)
        }
    }

    @Test
    fun handleResponseReturnsResponseFromImplementation()
    {
        runBlocking {
            val request = HandlerTestSupport.request()
            val connection = HandlerTestSupport.mockConnection()
            val expected = UUHttpResponse(request = request, parsedResponse = "done")
            val handler = object : UUHttpResponseHandler
            {
                override val successParser = UUBinaryStreamParser()
                override val errorParser = UUBinaryStreamParser()

                override suspend fun handleResponse(
                    request: UUHttpRequest,
                    urlConnection: HttpURLConnection,
                ): UUHttpResponse = expected
            }

            val result = handler.handleResponse(request, connection)

            assertSame(expected, result)
        }
    }

    @Test
    fun baseHandlerIsAUUHttpResponseHandler()
    {
        runBlocking {
            val handler: UUHttpResponseHandler = UUBaseResponseHandler()

            assertInstanceOf(UUHttpResponseHandler::class.java, handler)
            assertInstanceOf(UUBinaryStreamParser::class.java, handler.successParser)
            assertInstanceOf(UUBinaryStreamParser::class.java, handler.errorParser)
        }
    }
}

private class RecordingHandler(
    override val successParser: UUHttpStreamParser,
    override val errorParser: UUHttpStreamParser,
) : UUHttpResponseHandler
{
    override suspend fun handleResponse(
        request: UUHttpRequest,
        urlConnection: HttpURLConnection,
    ): UUHttpResponse = UUHttpResponse(request = request)
}
