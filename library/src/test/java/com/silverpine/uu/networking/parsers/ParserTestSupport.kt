package com.silverpine.uu.networking.parsers

import kotlinx.serialization.Serializable
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@Serializable
internal data class ParserTestPayload(
    val id: String = "",
    val count: Int = 0,
)

internal object ParserTestSupport
{
    fun bytes(vararg values: Int): ByteArray = values.map { it.toByte() }.toByteArray()

    fun stream(content: ByteArray): InputStream = ByteArrayInputStream(content)

    fun stream(content: String): InputStream = stream(content.toByteArray(Charsets.UTF_8))

    fun mockConnection(url: String = "https://example.com/resource.dat"): HttpURLConnection
    {
        val connection = mock<HttpURLConnection>()
        whenever(connection.url).thenReturn(URL(url))
        return connection
    }
}
