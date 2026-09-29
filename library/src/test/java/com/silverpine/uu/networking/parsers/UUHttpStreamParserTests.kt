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
        fun returnsValueFromLambda() = runBlocking<Unit> {
            val parser = uuHttpStreamParser { _, _ -> Result.success("parsed") }

            val result = parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection())

            assertEquals("parsed", result.getOrThrow())
        }

        @Test
        fun returnsNullWhenLambdaReturnsNull() = runBlocking<Unit> {
            val parser = uuHttpStreamParser { _, _ -> Result.success(null) }

            val result = parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection())

            assertNull(result.getOrThrow())
        }

        @Test
        fun passesStreamAndResponseToLambda() = runBlocking<Unit> {
            val stream = ParserTestSupport.stream("body")
            val connection = ParserTestSupport.mockConnection("https://api.example.com/items/1")
            var capturedStream: InputStream? = null
            var capturedConnection: HttpURLConnection? = null

            val parser = uuHttpStreamParser { s, r ->
                capturedStream = s
                capturedConnection = r
                Result.success(null)
            }

            parser.parse(stream, connection)

            assertSame(stream, capturedStream)
            assertSame(connection, capturedConnection)
        }

        @Test
        fun canReadStreamInsideSuspendLambda() = runBlocking<Unit> {
            val parser = uuHttpStreamParser { stream, _ ->
                Result.success(stream.bufferedReader().readText())
            }

            val result = parser.parse(ParserTestSupport.stream("hello"), ParserTestSupport.mockConnection())

            assertEquals("hello", result.getOrThrow())
        }

        @Test
        fun implementsUUHttpStreamParserInterface() = runBlocking<Unit> {
            val parser: UUHttpStreamParser = uuHttpStreamParser { _, _ -> Result.success(42) }

            assertTrue(parser is UUHttpStreamParser)
            assertEquals(42, parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection()).getOrThrow())
        }

        @Test
        fun multipleInvocationsAreIndependent() = runBlocking<Unit> {
            var callCount = 0
            val parser = uuHttpStreamParser { _, _ ->
                callCount++
                Result.success(callCount)
            }

            assertEquals(1, parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection()).getOrThrow())
            assertEquals(2, parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection()).getOrThrow())
        }
    }

    @Test
    fun factoryPreservesFailure() = runBlocking<Unit> {
        val failure = java.io.IOException("parse failed")
        val parser = uuHttpStreamParser { _, _ -> Result.failure(failure) }
        val result = parser.parse(ParserTestSupport.stream("body"), ParserTestSupport.mockConnection())
        assertSame(failure, result.exceptionOrNull())
    }

    @Nested
    inner class DirectImplementation
    {
        @Test
        fun anonymousObjectCanOverrideParse() = runBlocking<Unit> {
            val parser = object : UUHttpStreamParser
            {
                override suspend fun parse(
                    stream: InputStream,
                    response: HttpURLConnection,
                ): Result<Any?> = Result.success(stream.available())
            }

            val result = parser.parse(ParserTestSupport.stream("abc"), ParserTestSupport.mockConnection())

            assertEquals(3, result.getOrThrow())
        }
    }
}
