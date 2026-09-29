package com.silverpine.uu.networking.parsers

import kotlinx.coroutines.runBlocking
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
class UUTextResponseParserTests
{
    private lateinit var parser: UUTextResponseParser

    @BeforeEach
    fun setUp()
    {
        parser = UUTextResponseParser()
    }

    @Nested
    inner class SuccessfulDecoding
    {
        @Test
        fun decodesUtf8Text() = runBlocking<Unit> {
            val result = parser.parse(
                ParserTestSupport.stream("Hello, world!"),
                ParserTestSupport.mockConnection(),
            )

            assertInstanceOf(String::class.java, result.getOrThrow())
            assertEquals("Hello, world!", result.getOrThrow())
        }

        @Test
        fun decodesUnicodeText() = runBlocking<Unit> {
            val text = "銀虎 🐯 snow 雪"

            val result = parser.parse(ParserTestSupport.stream(text), ParserTestSupport.mockConnection())

            assertEquals(text, result.getOrThrow())
        }

        @Test
        fun decodesMultilineText() = runBlocking<Unit> {
            val text = "line one\nline two\r\nline three"

            val result = parser.parse(ParserTestSupport.stream(text), ParserTestSupport.mockConnection())

            assertEquals(text, result.getOrThrow())
        }

        @Test
        fun emptyStreamReturnsEmptyString() = runBlocking<Unit> {
            val result = parser.parse(
                ParserTestSupport.stream(ByteArray(0)),
                ParserTestSupport.mockConnection(),
            )

            assertEquals("", result.getOrThrow())
        }

        @Test
        fun decodesJsonAsPlainText() = runBlocking<Unit> {
            val json = """{"id":"abc","count":7}"""

            val result = parser.parse(ParserTestSupport.stream(json), ParserTestSupport.mockConnection())

            assertEquals(json, result.getOrThrow())
        }
    }

    @Nested
    inner class FailureHandling
    {
        @Test
        fun returnsFailureWhenStreamThrows() = runBlocking<Unit> {
            val failure = IOException("read failed")
            val failingStream = object : InputStream()
            {
                override fun read(): Int = throw failure
            }

            val result = parser.parse(failingStream, ParserTestSupport.mockConnection())

            org.junit.jupiter.api.Assertions.assertSame(failure, result.exceptionOrNull())
        }
    }
}
