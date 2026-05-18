package com.silverpine.uu.networking.parsers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
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
        fun decodesUtf8Text() = runBlocking {
            val result = parser.parse(
                ParserTestSupport.stream("Hello, world!"),
                ParserTestSupport.mockConnection(),
            )

            assertInstanceOf(String::class.java, result)
            assertEquals("Hello, world!", result)
        }

        @Test
        fun decodesUnicodeText() = runBlocking {
            val text = "銀虎 🐯 snow 雪"

            val result = parser.parse(ParserTestSupport.stream(text), ParserTestSupport.mockConnection())

            assertEquals(text, result)
        }

        @Test
        fun decodesMultilineText() = runBlocking {
            val text = "line one\nline two\r\nline three"

            val result = parser.parse(ParserTestSupport.stream(text), ParserTestSupport.mockConnection())

            assertEquals(text, result)
        }

        @Test
        fun emptyStreamReturnsEmptyString() = runBlocking {
            val result = parser.parse(
                ParserTestSupport.stream(ByteArray(0)),
                ParserTestSupport.mockConnection(),
            )

            assertEquals("", result)
        }

        @Test
        fun decodesJsonAsPlainText() = runBlocking {
            val json = """{"id":"abc","count":7}"""

            val result = parser.parse(ParserTestSupport.stream(json), ParserTestSupport.mockConnection())

            assertEquals(json, result)
        }
    }

    @Nested
    inner class FailureHandling
    {
        @Test
        fun returnsNullWhenStreamThrows() = runBlocking {
            val failingStream = object : InputStream()
            {
                override fun read(): Int = throw IOException("read failed")
            }

            val result = parser.parse(failingStream, ParserTestSupport.mockConnection())

            assertNull(result)
        }
    }
}
