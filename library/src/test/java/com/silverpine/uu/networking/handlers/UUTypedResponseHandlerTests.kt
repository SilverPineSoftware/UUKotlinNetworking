package com.silverpine.uu.networking.handlers

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.networking.UUNetworkErrorCode
import com.silverpine.uu.networking.parsers.ParserTestPayload
import com.silverpine.uu.networking.parsers.UUTypedStreamParser
import com.silverpine.uu.networking.uuNetworkErrorCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
class UUTypedResponseHandlerTests
{
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @BeforeEach
    fun setUp()
    {
        UUJson.init(UUKotlinXJsonProvider(json))
    }

    @AfterEach
    fun tearDown()
    {
        UUJson.init(UUKotlinXJsonProvider(Json.Default))
    }

    @Nested
    inner class ParserConfiguration
    {
        @Test
        fun wiresTypedStreamParsersForSuccessAndError()
        {
            runBlocking {
                val handler = UUTypedResponseHandler(
                    ParserTestPayload::class.java,
                    ParserTestPayload::class.java,
                )

                assertInstanceOf(UUTypedStreamParser::class.java, handler.successParser)
                assertInstanceOf(UUTypedStreamParser::class.java, handler.errorParser)
            }
        }
    }

    @Nested
    inner class SuccessfulResponses
    {
        @Test
        fun deserializesSuccessBodyIntoSuccessType()
        {
            runBlocking {
                val handler = UUTypedResponseHandler(
                    ParserTestPayload::class.java,
                    ParserTestPayload::class.java,
                )
                val body = """{"id":"typed-ok","count":3}""".toByteArray()
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 200,
                    body = body,
                    contentType = "application/json",
                )

                val response = handler.handleResponse(HandlerTestSupport.request(), connection)

                assertNull(response.error)
                val payload = response.parsedResponse as ParserTestPayload
                assertEquals("typed-ok", payload.id)
                assertEquals(3, payload.count)
            }
        }
    }

    @Nested
    inner class ErrorResponses
    {
        @Test
        fun mapsNonSuccessStatusToNetworkError()
        {
            runBlocking {
                val handler = UUTypedResponseHandler(
                    ParserTestPayload::class.java,
                    ParserTestPayload::class.java,
                )
                val body = """{"id":"api-error","count":0}""".toByteArray()
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 400,
                    body = body,
                    contentType = "application/json",
                )

                val response = handler.handleResponse(HandlerTestSupport.request(), connection)

                assertEquals(UUNetworkErrorCode.HTTP_FAILURE, response.error?.uuNetworkErrorCode())
                val payload = response.parsedResponse as ParserTestPayload
                assertEquals("api-error", payload.id)
            }
        }

        @Test
        fun maps401ToAuthorizationNeeded()
        {
            runBlocking {
                val handler = UUTypedResponseHandler(
                    ParserTestPayload::class.java,
                    ParserTestPayload::class.java,
                )
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 401,
                    body = """{"id":"auth","count":0}""".toByteArray(),
                    contentType = "application/json",
                )

                val response = handler.handleResponse(HandlerTestSupport.request(), connection)

                assertEquals(UUNetworkErrorCode.AUTHORIZATION_NEEDED, response.error?.uuNetworkErrorCode())
            }
        }
    }
}
