package com.silverpine.uu.networking.parsers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.InputStream
import java.net.HttpURLConnection

@ExtendWith(RobolectricExtension::class)
class UUHttpStreamParserTests
{
    @Nested
    inner class UuHttpStreamParserFactory
    {
        @Test
        fun returnsValueFromLambda() = runBlocking {
            val parser = uuHttpStreamParser { _, _ -> "parsed" }

            val result = parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection())

            assertEquals("parsed", result)
        }

        @Test
        fun returnsNullWhenLambdaReturnsNull() = runBlocking {
            val parser = uuHttpStreamParser { _, _ -> null }

            val result = parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection())

            assertNull(result)
        }

        @Test
        fun passesStreamAndResponseToLambda() = runBlocking {
            val stream = ParserTestSupport.stream("body")
            val connection = ParserTestSupport.mockConnection("https://api.example.com/items/1")
            var capturedStream: InputStream? = null
            var capturedConnection: HttpURLConnection? = null

            val parser = uuHttpStreamParser { s, r ->
                capturedStream = s
                capturedConnection = r
                null
            }

            parser.parse(stream, connection)

            assertSame(stream, capturedStream)
            assertSame(connection, capturedConnection)
        }

        @Test
        fun canReadStreamInsideSuspendLambda() = runBlocking {
            val parser = uuHttpStreamParser { stream, _ ->
                stream.bufferedReader().readText()
            }

            val result = parser.parse(ParserTestSupport.stream("hello"), ParserTestSupport.mockConnection())

            assertEquals("hello", result)
        }

        @Test
        fun implementsUUHttpStreamParserInterface() = runBlocking {
            val parser: UUHttpStreamParser = uuHttpStreamParser { _, _ -> 42 }

            assertTrue(parser is UUHttpStreamParser)
            assertEquals(42, parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection()))
        }

        @Test
        fun multipleInvocationsAreIndependent() = runBlocking {
            var callCount = 0
            val parser = uuHttpStreamParser { _, _ ->
                callCount++
                callCount
            }

            assertEquals(1, parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection()))
            assertEquals(2, parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection()))
        }
    }

    @Nested
    inner class DirectImplementation
    {
        @Test
        fun anonymousObjectCanOverrideParse() = runBlocking {
            val parser = object : UUHttpStreamParser
            {
                override suspend fun parse(
                    stream: InputStream,
                    response: HttpURLConnection,
                ): Any? = stream.available()
            }

            val result = parser.parse(ParserTestSupport.stream("abc"), ParserTestSupport.mockConnection())

            assertEquals(3, result)
        }
    }
}
