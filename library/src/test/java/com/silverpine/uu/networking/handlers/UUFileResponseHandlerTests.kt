package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.UUNetworkErrorCode
import com.silverpine.uu.networking.parsers.UUBinaryStreamParser
import com.silverpine.uu.networking.parsers.UUDownloadFileStreamParser
import com.silverpine.uu.networking.uuNetworkErrorCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.File

@ExtendWith(RobolectricExtension::class)
class UUFileResponseHandlerTests
{
    @TempDir
    lateinit var downloadFolder: File

    private lateinit var handler: UUFileResponseHandler

    @BeforeEach
    fun setUp()
    {
        handler = UUFileResponseHandler(downloadFolder)
    }

    @Nested
    inner class ParserConfiguration
    {
        @Test
        fun successParserIsDownloadFileStreamParser()
        {
            runBlocking {
                assertInstanceOf(UUDownloadFileStreamParser::class.java, handler.successParser)
            }
        }

        @Test
        fun errorParserRemainsBinaryStreamParser()
        {
            runBlocking {
                assertInstanceOf(UUBinaryStreamParser::class.java, handler.errorParser)
            }
        }
    }

    @Nested
    inner class SuccessfulDownload
    {
        @Test
        fun writesResponseBodyToFileInDownloadFolder()
        {
            runBlocking {
                val payload = "downloaded bytes".toByteArray()
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 200,
                    body = payload,
                    url = "https://cdn.example.com/files/report.bin",
                )
                val request = HandlerTestSupport.request()

                val response = handler.handleResponse(request, connection)

                assertNull(response.error)
                val file = response.parsedResponse as File
                assertEquals(downloadFolder, file.parentFile)
                assertEquals("report.bin", file.name)
                assertArrayEquals(payload, file.readBytes())
            }
        }
    }

    @Nested
    inner class ErrorResponses
    {
        @Test
        fun nonSuccessStatusProducesNetworkErrorWithoutFile()
        {
            runBlocking {
                val connection = HandlerTestSupport.mockConnection(
                    statusCode = 500,
                    body = "server error".toByteArray(),
                )

                val response = handler.handleResponse(HandlerTestSupport.request(), connection)

                assertEquals(UUNetworkErrorCode.HTTP_FAILURE, response.error?.uuNetworkErrorCode())
                assertArrayEquals("server error".toByteArray(), response.parsedResponse as ByteArray)
            }
        }
    }
}
