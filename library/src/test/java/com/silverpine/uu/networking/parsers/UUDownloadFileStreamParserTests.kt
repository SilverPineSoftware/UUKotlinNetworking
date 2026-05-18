package com.silverpine.uu.networking.parsers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.File
import java.io.IOException
import java.io.InputStream

@ExtendWith(RobolectricExtension::class)
class UUDownloadFileStreamParserTests
{
    @TempDir
    lateinit var downloadFolder: File

    private lateinit var parser: UUDownloadFileStreamParser

    @BeforeEach
    fun setUp()
    {
        parser = UUDownloadFileStreamParser(downloadFolder)
    }

    @Nested
    inner class SuccessfulDownloads
    {
        @Test
        fun writesPayloadToFileNamedFromUrlPath() = runBlocking {
            val payload = "file contents".toByteArray()
            val connection = ParserTestSupport.mockConnection(
                "https://cdn.example.com/downloads/archive.zip",
            )

            val result = parser.parse(ParserTestSupport.stream(payload), connection)

            assertInstanceOf(File::class.java, result)
            val destFile = result as File
            assertEquals(downloadFolder, destFile.parentFile)
            assertEquals("archive.zip", destFile.name)
            assertTrue(destFile.exists())
            assertArrayEquals(payload, destFile.readBytes())
        }

        @Test
        fun usesLastPathSegmentAsFileName() = runBlocking {
            val connection = ParserTestSupport.mockConnection(
                "https://example.com/a/b/c/report.pdf",
            )

            val result = parser.parse(
                ParserTestSupport.stream("pdf-bytes"),
                connection,
            ) as File

            assertEquals("report.pdf", result.name)
            assertEquals("pdf-bytes", result.readText())
        }

        @Test
        fun overwritesExistingFileWithSameName() = runBlocking {
            val existing = File(downloadFolder, "data.bin").apply { writeText("old") }
            val connection = ParserTestSupport.mockConnection("https://example.com/data.bin")

            val result = parser.parse(
                ParserTestSupport.stream("new"),
                connection,
            ) as File

            assertEquals(existing.absolutePath, result.absolutePath)
            assertEquals("new", result.readText())
        }

        @Test
        fun emptyStreamCreatesEmptyFile() = runBlocking {
            val connection = ParserTestSupport.mockConnection("https://example.com/empty.dat")

            val result = parser.parse(
                ParserTestSupport.stream(ByteArray(0)),
                connection,
            ) as File

            assertTrue(result.exists())
            assertEquals(0, result.length())
        }

        @Test
        fun writesBinaryPayloadWithoutCorruption() = runBlocking {
            val payload = byteArrayOf(0x00, 0x10, 0xFF.toByte(), 0x7F)

            val result = parser.parse(
                ParserTestSupport.stream(payload),
                ParserTestSupport.mockConnection("https://example.com/raw.bin"),
            ) as File

            assertArrayEquals(payload, result.readBytes())
        }

        @Test
        fun writesLargePayload() = runBlocking {
            val payload = ByteArray(50_000) { (it % 256).toByte() }

            val result = parser.parse(
                ParserTestSupport.stream(payload),
                ParserTestSupport.mockConnection("https://example.com/large.bin"),
            ) as File

            assertArrayEquals(payload, result.readBytes())
        }
    }

    @Nested
    inner class UrlHandling
    {
        @Test
        fun fileNameWithoutExtensionIsPreserved() = runBlocking {
            val connection = ParserTestSupport.mockConnection("https://example.com/README")

            val result = parser.parse(
                ParserTestSupport.stream("readme"),
                connection,
            ) as File

            assertEquals("README", result.name)
        }

        @Test
        fun queryStringDoesNotAffectFileName() = runBlocking {
            val connection = ParserTestSupport.mockConnection(
                "https://example.com/assets/logo.png?version=2",
            )

            val result = parser.parse(
                ParserTestSupport.stream("png"),
                connection,
            ) as File

            assertEquals("logo.png", result.name)
        }
    }

    @Nested
    inner class DownloadFolder
    {
        @Test
        fun createsFileInsideConfiguredFolder() = runBlocking {
            val nestedFolder = File(downloadFolder, "nested").apply { mkdirs() }
            val nestedParser = UUDownloadFileStreamParser(nestedFolder)

            val result = nestedParser.parse(
                ParserTestSupport.stream("nested-content"),
                ParserTestSupport.mockConnection("https://example.com/nested.txt"),
            ) as File

            assertEquals(nestedFolder.absolutePath, result.parentFile?.absolutePath)
            assertFalse(File(downloadFolder, "nested.txt").exists())
            assertEquals("nested-content", result.readText())
        }
    }

    @Nested
    inner class FailureHandling
    {
        @Test
        fun doesNotThrowWhenStreamReadFails() = runBlocking {
            val failingStream = object : InputStream()
            {
                override fun read(): Int = throw IOException("read failed")
            }

            val result = runCatching {
                parser.parse(
                    failingStream,
                    ParserTestSupport.mockConnection("https://example.com/fail.bin"),
                )
            }

            assertTrue(result.isSuccess)
            val file = result.getOrNull() as File
            assertTrue(file.exists())
            assertEquals(0, file.length())
        }
    }
}
