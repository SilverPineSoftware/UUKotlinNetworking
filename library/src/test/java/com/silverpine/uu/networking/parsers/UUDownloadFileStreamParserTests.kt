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
        fun writesPayloadToFileNamedFromUrlPath() = runBlocking<Unit> {
            val payload = "file contents".toByteArray()
            val connection = ParserTestSupport.mockConnection(
                "https://cdn.example.com/downloads/archive.zip",
            )

            val result = parser.parse(ParserTestSupport.stream(payload), connection)

            assertInstanceOf(File::class.java, result.getOrThrow())
            val destFile = result.getOrThrow() as File
            assertEquals(downloadFolder, destFile.parentFile)
            assertEquals("archive.zip", destFile.name)
            assertTrue(destFile.exists())
            assertArrayEquals(payload, destFile.readBytes())
        }

        @Test
        fun usesLastPathSegmentAsFileName() = runBlocking<Unit> {
            val connection = ParserTestSupport.mockConnection(
                "https://example.com/a/b/c/report.pdf",
            )

            val result = parser.parse(
                ParserTestSupport.stream("pdf-bytes"),
                connection,
            ).getOrThrow() as File

            assertEquals("report.pdf", result.name)
            assertEquals("pdf-bytes", result.readText())
        }

        @Test
        fun overwritesExistingFileWithSameName() = runBlocking<Unit> {
            val existing = File(downloadFolder, "data.bin").apply { writeText("old") }
            val connection = ParserTestSupport.mockConnection("https://example.com/data.bin")

            val result = parser.parse(
                ParserTestSupport.stream("new"),
                connection,
            ).getOrThrow() as File

            assertEquals(existing.absolutePath, result.absolutePath)
            assertEquals("new", result.readText())
        }

        @Test
        fun emptyStreamCreatesEmptyFile() = runBlocking<Unit> {
            val connection = ParserTestSupport.mockConnection("https://example.com/empty.dat")

            val result = parser.parse(
                ParserTestSupport.stream(ByteArray(0)),
                connection,
            ).getOrThrow() as File

            assertTrue(result.exists())
            assertEquals(0, result.length())
        }

        @Test
        fun writesBinaryPayloadWithoutCorruption() = runBlocking<Unit> {
            val payload = byteArrayOf(0x00, 0x10, 0xFF.toByte(), 0x7F)

            val result = parser.parse(
                ParserTestSupport.stream(payload),
                ParserTestSupport.mockConnection("https://example.com/raw.bin"),
            ).getOrThrow() as File

            assertArrayEquals(payload, result.readBytes())
        }

        @Test
        fun writesLargePayload() = runBlocking<Unit> {
            val payload = ByteArray(50_000) { (it % 256).toByte() }

            val result = parser.parse(
                ParserTestSupport.stream(payload),
                ParserTestSupport.mockConnection("https://example.com/large.bin"),
            ).getOrThrow() as File

            assertArrayEquals(payload, result.readBytes())
        }
    }

    @Nested
    inner class UrlHandling
    {
        @Test
        fun fileNameWithoutExtensionIsPreserved() = runBlocking<Unit> {
            val connection = ParserTestSupport.mockConnection("https://example.com/README")

            val result = parser.parse(
                ParserTestSupport.stream("readme"),
                connection,
            ).getOrThrow() as File

            assertEquals("README", result.name)
        }

        @Test
        fun queryStringDoesNotAffectFileName() = runBlocking<Unit> {
            val connection = ParserTestSupport.mockConnection(
                "https://example.com/assets/logo.png?version=2",
            )

            val result = parser.parse(
                ParserTestSupport.stream("png"),
                connection,
            ).getOrThrow() as File

            assertEquals("logo.png", result.name)
        }
    }

    @Nested
    inner class DownloadFolder
    {
        @Test
        fun createsFileInsideConfiguredFolder() = runBlocking<Unit> {
            val nestedFolder = File(downloadFolder, "nested").apply { mkdirs() }
            val nestedParser = UUDownloadFileStreamParser(nestedFolder)

            val result = nestedParser.parse(
                ParserTestSupport.stream("nested-content"),
                ParserTestSupport.mockConnection("https://example.com/nested.txt"),
            ).getOrThrow() as File

            assertEquals(nestedFolder.absolutePath, result.parentFile?.absolutePath)
            assertFalse(File(downloadFolder, "nested.txt").exists())
            assertEquals("nested-content", result.readText())
        }
    }

    @Test
    fun fileCreationFailureIsReturned() = runBlocking<Unit> {
        val parser = UUDownloadFileStreamParser(File(downloadFolder, "missing/directory"))
        val result = parser.parse(ParserTestSupport.stream("body"), ParserTestSupport.mockConnection())
        assertInstanceOf(IOException::class.java, result.exceptionOrNull())
    }

    @Nested
    inner class FailureHandling
    {
        @Test
        fun doesNotThrowWhenStreamReadFails() = runBlocking<Unit> {
            val failure = IOException("read failed")
            val failingStream = object : InputStream()
            {
                override fun read(): Int = throw failure
            }

            val result = parser.parse(
                failingStream,
                ParserTestSupport.mockConnection("https://example.com/fail.bin"),
            )
            org.junit.jupiter.api.Assertions.assertSame(failure, result.exceptionOrNull())

        }
    }
}
