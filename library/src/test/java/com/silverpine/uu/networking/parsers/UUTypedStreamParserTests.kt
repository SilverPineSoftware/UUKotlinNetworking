package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.IOException
import java.io.InputStream

@ExtendWith(RobolectricExtension::class)
class UUTypedStreamParserTests
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
    inner class SuccessfulParsing
    {
        @Test
        fun parsesJsonObjectIntoTargetClass() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)
            val body = """{"id":"item-42","count":99}"""

            val result = parser.parse(ParserTestSupport.stream(body), ParserTestSupport.mockConnection())

            assertInstanceOf(ParserTestPayload::class.java, result.getOrThrow())
            val payload = result.getOrThrow() as ParserTestPayload
            assertEquals("item-42", payload.id)
            assertEquals(99, payload.count)
        }

        @Test
        fun ignoresUnknownJsonKeys() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)
            val body = """{"id":"x","count":1,"extra":"ignored"}"""

            val result = parser.parse(ParserTestSupport.stream(body), ParserTestSupport.mockConnection())

            assertEquals(ParserTestPayload(id = "x", count = 1), result.getOrThrow())
        }

        @Test
        fun parsesMinimalJsonObject() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)

            val result = parser.parse(
                ParserTestSupport.stream("""{}"""),
                ParserTestSupport.mockConnection(),
            )

            assertEquals(ParserTestPayload(), result.getOrThrow())
        }
    }

    @Nested
    inner class FailureHandling
    {
        @Test
        fun returnsFailureForMalformedJson() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)

            val result = parser.parse(
                ParserTestSupport.stream("{not json"),
                ParserTestSupport.mockConnection(),
            )

            assertInstanceOf(Exception::class.java, result.exceptionOrNull())
        }

        @Test
        fun returnsFailureForJsonTypeMismatch() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)

            val result = parser.parse(
                ParserTestSupport.stream("""{"id":"x","count":"not-a-number"}"""),
                ParserTestSupport.mockConnection(),
            )

            assertInstanceOf(Exception::class.java, result.exceptionOrNull())
        }

        @Test
        fun returnsFailureForEmptyStream() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)

            val result = parser.parse(
                ParserTestSupport.stream(ByteArray(0)),
                ParserTestSupport.mockConnection(),
            )

            assertInstanceOf(Exception::class.java, result.exceptionOrNull())
        }

        @Test
        fun returnsFailureWhenStreamThrows() = runBlocking<Unit> {
            val parser = UUTypedStreamParser(ParserTestPayload::class.java)
            val failure = IOException("read failed")
            val failingStream = object : InputStream()
            {
                override fun read(): Int = throw failure
            }

            val result = parser.parse(failingStream, ParserTestSupport.mockConnection())

            org.junit.jupiter.api.Assertions.assertSame(failure, result.exceptionOrNull())
        }
    }

    @Nested
    inner class Subclassing
    {
        @Test
        fun openClassCanBeExtended() = runBlocking<Unit> {
            val customParser = object : UUTypedStreamParser<ParserTestPayload>(ParserTestPayload::class.java)
            {
                override suspend fun parse(
                    stream: InputStream,
                    response: java.net.HttpURLConnection,
                ): Result<Any?> = Result.success(ParserTestPayload(id = "custom", count = -1))
            }

            val result = customParser.parse(
                ParserTestSupport.stream("""{"id":"ignored"}"""),
                ParserTestSupport.mockConnection(),
            )

            assertEquals(ParserTestPayload(id = "custom", count = -1), result.getOrThrow())
        }
    }
}
