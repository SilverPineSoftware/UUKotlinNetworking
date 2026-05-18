package com.silverpine.uu.networking

import android.net.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.net.URL

@ExtendWith(RobolectricExtension::class)
class UUHttpRequestTests
{
    private fun makeRequest(
        base: String,
        paths: List<String> = emptyList(),
        query: Map<String, String> = emptyMap(),
    ): UUHttpRequest
    {
        val p = arrayListOf<String>().apply { addAll(paths) }
        val q = hashMapOf<String, String>().apply { putAll(query) }
        return UUHttpRequest(url = base, path = p, query = q)
    }

    private fun URL.asUri(): Uri = Uri.parse(toString())

    private fun assertQueryContainsAll(url: URL, expected: Map<String, String>)
    {
        val uri = url.asUri()
        expected.forEach { (k, v) ->
            val values = uri.getQueryParameters(k)
            assertTrue(values.isNotEmpty(), "Missing query key: $k in $uri")
            assertTrue(
                values.contains(v),
                "Expected $k to contain value '$v' but got $values in $uri",
            )
        }
    }

    @Test
    fun constructor_assignsUniqueId()
    {
        val req = UUHttpRequest("")
        assertNotNull(req.id)
    }

    @Test
    fun baseUrlPassthroughNoPathNoQuery()
    {
        val req = makeRequest("https://example.com")
        val url = req.toURL
        assertEquals("https://example.com", url.toString())
    }

    @Test
    fun appendsSimplePathSegments()
    {
        val req = makeRequest(
            base = "https://example.com",
            paths = listOf("v1", "users", "42"),
        )
        val url = req.toURL
        assertEquals("https://example.com/v1/users/42", url.toString())
    }

    @Test
    fun baseWithTrailingSlashAndPath()
    {
        val req = makeRequest(
            base = "https://example.com/",
            paths = listOf("a", "b"),
        )
        val url = req.toURL
        assertEquals("https://example.com/a/b", url.toString())
    }

    @Test
    fun pathSegmentsWithInternalSlashAreEncoded()
    {
        val req = makeRequest(
            base = "https://example.com",
            paths = listOf("foo/bar", "baz"),
        )
        val url = req.toURL
        assertEquals("https://example.com/foo%2Fbar/baz", url.toString())
    }

    @Test
    fun basicQueryParamsAddedOrderAgnostic()
    {
        val req = makeRequest(
            base = "https://example.com",
            query = mapOf("q" to "cat dog", "format" to "json"),
        )
        val url = req.toURL
        assertQueryContainsAll(url, mapOf("q" to "cat dog", "format" to "json"))

        val qs = url.asUri().encodedQuery ?: ""
        assertTrue(qs.contains("format=json"))
        assertTrue(qs.contains("q=cat%20dog"))
    }

    @Test
    fun baseAlreadyHasQueryParamsNewOnesAreAppended()
    {
        val req = makeRequest(
            base = "https://example.com/api?lang=en",
            paths = listOf("v1"),
            query = mapOf("q" to "x"),
        )
        val url = req.toURL
        val uri = url.asUri()

        assertEquals("/api/v1", uri.encodedPath)
        assertQueryContainsAll(url, mapOf("lang" to "en", "q" to "x"))
    }

    @Test
    fun unicodeInPathAndQueryRoundTrips()
    {
        val req = makeRequest(
            base = "https://example.com",
            paths = listOf("雪", "🐯"),
            query = mapOf("name" to "銀 虎"),
        )
        val url = req.toURL
        val uri = url.asUri()

        assertEquals(listOf("雪", "🐯"), uri.pathSegments)
        assertEquals(listOf("銀 虎"), uri.getQueryParameters("name"))
    }

    @Test
    fun reservedCharactersInQueryAreEncodedPerAndroidUriRules()
    {
        val req = makeRequest(
            base = "https://example.com",
            query = mapOf("k*~" to "* ~ !", "a&b" to "1=2"),
        )
        val url = req.toURL
        val uri = url.asUri()
        val qs = uri.encodedQuery.orEmpty()

        val names = uri.queryParameterNames
        assertTrue(names.contains("k*~"), "Missing key k*~ in $uri")
        assertTrue(names.contains("a&b"), "Missing key a&b in $uri")

        assertEquals("* ~ !", uri.getQueryParameter("k*~"))
        assertEquals("1=2", uri.getQueryParameter("a&b"))

        assertTrue(qs.contains("%20"))
        assertTrue(
            qs.contains("a%26b=1%3D2"),
            "Expected encoded key/value for a&b in $qs",
        )
        assertTrue(
            qs.contains("k*~="),
            "Expected k*~ pair in encoded query: $qs",
        )
    }

    @Test
    fun baseWithPortAndFragmentPreserved()
    {
        val req = makeRequest(
            base = "https://example.com:8443/base#frag",
            paths = listOf("p"),
            query = mapOf("x" to "y"),
        )
        val url = req.toURL
        val uri = url.asUri()

        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.host)
        assertEquals(8443, uri.port)
        assertEquals("/base/p", uri.encodedPath)
        assertEquals("frag", uri.fragment)
        assertQueryContainsAll(url, mapOf("x" to "y"))
    }

    @Test
    fun emptyPathAndQueryKeepsBaseIntact()
    {
        val req = makeRequest("https://example.com/base")
        val url = req.toURL
        assertEquals("https://example.com/base", url.toString())
    }
}
