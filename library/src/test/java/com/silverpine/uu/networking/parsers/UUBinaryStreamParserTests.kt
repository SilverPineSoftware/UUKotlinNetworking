package com.silverpine.uu.networking.parsers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.IOException
import java.io.InputStream

@ExtendWith(RobolectricExtension::class)
class UUBinaryStreamParserTests
{
    private lateinit var parser: UUBinaryStreamParser

    @BeforeEach
    fun setUp()
    {
        parser = UUBinaryStreamParser()
    }

    @Nested
    inner class SuccessfulReads
    {
        @Test
        fun readsSingleBytePayload() = runBlocking {
            val payload = ParserTestSupport.bytes(0xAB)

            val result = parser.parse(ParserTestSupport.stream(payload), ParserTestSupport.mockConnection())

            assertInstanceOf(ByteArray::class.java, result)
            assertArrayEquals(payload, result as ByteArray)
        }

        @Test
        fun readsMultiBytePayload() = runBlocking {
            val payload = "binary payload".toByteArray()

            val result = parser.parse(ParserTestSupport.stream(payload), ParserTestSupport.mockConnection())

            assertArrayEquals(payload, result as ByteArray)
        }

        @Test
        fun readsPayloadLargerThanDefaultBuffer() = runBlocking {
            val payload = ByteArray(25_000) { (it % 256).toByte() }

            val result = parser.parse(ParserTestSupport.stream(payload), ParserTestSupport.mockConnection())

            assertArrayEquals(payload, result as ByteArray)
        }

        @Test
        fun emptyStreamReturnsEmptyByteArray() = runBlocking {
            val result = parser.parse(ParserTestSupport.stream(ByteArray(0)), ParserTestSupport.mockConnection())

            assertNotNull(result)
            assertArrayEquals(ByteArray(0), result as ByteArray)
        }

        @Test
        fun preservesNullBytesInPayload() = runBlocking {
            val payload = byteArrayOf(0x00, 0x01, 0x00, 0xFF.toByte())

            val result = parser.parse(ParserTestSupport.stream(payload), ParserTestSupport.mockConnection())

            assertArrayEquals(payload, result as ByteArray)
        }

        @Test
        fun ignoresHttpConnectionArgument() = runBlocking {
            val connection = ParserTestSupport.mockConnection("https://other.example.com/file.bin")

            val result = parser.parse(ParserTestSupport.stream("x"), connection)

            assertArrayEquals("x".toByteArray(), result as ByteArray)
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

    @Nested
    inner class Subclassing
    {
        @Test
        fun openClassCanBeExtended() = runBlocking {
            val customParser = object : UUBinaryStreamParser()
            {
                override suspend fun parse(
                    stream: java.io.InputStream,
                    response: java.net.HttpURLConnection,
                ): Any? = "override"
            }

            val result = customParser.parse(
                ParserTestSupport.stream("ignored"),
                ParserTestSupport.mockConnection(),
            )

            assertEquals("override", result)
        }
    }
}
