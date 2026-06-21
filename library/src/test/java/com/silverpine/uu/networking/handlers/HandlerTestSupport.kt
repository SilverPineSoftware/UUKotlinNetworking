package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.UUHttpRequest
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

internal object HandlerTestSupport
{
    fun request(url: String = "https://api.example.com/resource"): UUHttpRequest =
        UUHttpRequest(url = url).apply {
            loggingMode = emptyArray()
        }

    fun mockConnection(
        statusCode: Int = 200,
        body: ByteArray = "ok".toByteArray(),
        contentEncoding: String? = null,
        url: String = "https://api.example.com/resource",
        contentType: String = "application/octet-stream",
    ): HttpURLConnection
    {
        val connection = mock<HttpURLConnection>()
        whenever(connection.responseCode).thenReturn(statusCode)
        whenever(connection.url).thenReturn(URL(url))
        whenever(connection.contentType).thenReturn(contentType)
        whenever(connection.contentLength).thenReturn(body.size)
        whenever(connection.contentEncoding).thenReturn(contentEncoding)
        whenever(connection.headerFields).thenReturn(emptyMap())

        val bodyStream = ByteArrayInputStream(body)
        if (statusCode.uuIsHttpSuccessForTest())
        {
            whenever(connection.inputStream).thenReturn(bodyStream)
        }
        else
        {
            whenever(connection.errorStream).thenReturn(bodyStream)
        }

        return connection
    }

    fun gzipBody(uncompressed: ByteArray): ByteArray
    {
        val output = java.io.ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            gzip.write(uncompressed)
        }
        return output.toByteArray()
    }

    private fun Int.uuIsHttpSuccessForTest(): Boolean = this in 200..299
}
