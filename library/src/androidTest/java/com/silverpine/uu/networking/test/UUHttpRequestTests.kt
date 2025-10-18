package com.silverpine.uu.networking.test

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.networking.UUHttpRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.net.URL

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UUHttpRequestTests
{

    // --- Helpers -------------------------------------------------------------

    private fun makeRequest(
        base: String,
        paths: List<String> = emptyList(),
        query: Map<String, String> = emptyMap()
    ): UUHttpRequest {
        val p = arrayListOf<String>().apply { addAll(paths) }
        val q = hashMapOf<String, String>().apply { putAll(query) }
        return UUHttpRequest(url = base, path = p, query = q)
    }

    private fun URL.asUri(): Uri = Uri.parse(toString())

    private fun assertQueryContainsAll(url: URL, expected: Map<String, String>) {
        val uri = url.asUri()
        // Verify presence & values; ignore ordering
        expected.forEach { (k, v) ->
            val values = uri.getQueryParameters(k) // Android API returns decoded values
            assertTrue("Missing query key: $k in $uri", values.isNotEmpty())
            assertTrue(
                "Expected $k to contain value '$v' but got $values in $uri",
                values.contains(v)
            )
        }
    }

    // --- Tests ---------------------------------------------------------------

    @Test
    fun base_url_passthrough_no_path_no_query()
    {
        val req = makeRequest("https://example.com")
        val url = req.toURL
        assertEquals("https://example.com", url.toString())
    }

    @Test
    fun appends_simple_path_segments()
    {
        val req = makeRequest(
            base = "https://example.com",
            paths = listOf("v1", "users", "42")
        )
        val url = req.toURL
        assertEquals("https://example.com/v1/users/42", url.toString())
    }

    @Test
    fun base_with_trailing_slash_and_path()
    {
        val req = makeRequest(
            base = "https://example.com/",
            paths = listOf("a", "b")
        )
        val url = req.toURL
        assertEquals("https://example.com/a/b", url.toString())
    }

    @Test
    fun path_segments_with_internal_slash_are_encoded()
    {
        // appendPath treats the entire string as one segment; '/' becomes %2F
        val req = makeRequest(
            base = "https://example.com",
            paths = listOf("foo/bar", "baz")
        )
        val url = req.toURL
        assertEquals("https://example.com/foo%2Fbar/baz", url.toString())
    }

    @Test
    fun basic_query_params_added_order_agnostic()
    {
        val req = makeRequest(
            base = "https://example.com",
            query = mapOf("q" to "cat dog", "format" to "json")
        )
        val url = req.toURL
        assertQueryContainsAll(url, mapOf("q" to "cat dog", "format" to "json"))

        // Ensure URL has a query and includes both keys
        val qs = url.asUri().encodedQuery ?: ""
        assertTrue(qs.contains("format=json"))
        // Space encoded as %20 by Uri.Builder
        assertTrue(qs.contains("q=cat%20dog"))
    }

    @Test
    fun base_already_has_query_params_new_ones_are_appended()
    {
        val req = makeRequest(
            base = "https://example.com/api?lang=en",
            paths = listOf("v1"),
            query = mapOf("q" to "x")
        )
        val url = req.toURL
        val uri = url.asUri()

        assertEquals("/api/v1", uri.encodedPath)
        assertQueryContainsAll(url, mapOf("lang" to "en", "q" to "x"))
    }

    @Test
    fun unicode_in_path_and_query_round_trips()
    {
        val req = makeRequest(
            base = "https://example.com",
            paths = listOf("雪", "🐯"),
            query = mapOf("name" to "銀 虎")
        )
        val url = req.toURL
        val uri = url.asUri()

        // Path is encoded but decodes back to original segments
        val segs = uri.pathSegments
        assertEquals(listOf("雪", "🐯"), segs)

        // Query decoder returns original Unicode value
        assertEquals(listOf("銀 虎"), uri.getQueryParameters("name"))
    }

    @Test
    fun reserved_characters_in_query_are_encoded_per_android_uri_rules()
    {
        val req = makeRequest(
            base = "https://example.com",
            query = mapOf("k*~" to "* ~ !", "a&b" to "1=2")
        )
        val url = req.toURL
        val uri = url.asUri()
        val qs = uri.encodedQuery.orEmpty()

        // --- Decoded assertions (source of truth for presence/values) ---
        // Use queryParameterNames to assert presence (more robust for odd chars in keys)
        val names = uri.queryParameterNames
        assertTrue("Missing key k*~ in $uri", names.contains("k*~"))
        assertTrue("Missing key a&b in $uri", names.contains("a&b"))

        // Values are decoded by Uri API
        assertEquals("* ~ !", uri.getQueryParameter("k*~"))
        assertEquals("1=2", uri.getQueryParameter("a&b"))

        // --- Encoded spot checks (order-agnostic) ---
        // Spaces encoded as %20
        assertTrue(qs.contains("%20"))

        // Key containing '&' must be encoded in the key as %26; '=' in value must be %3D
        assertTrue(
            "Expected encoded key/value for a&b in $qs",
            qs.contains("a%26b=1%3D2")
        )

        // '*' typically remains unencoded in the key; ensure the pair is present
        assertTrue(
            "Expected k*~ pair in encoded query: $qs",
            qs.contains("k*~=")
        )

        // Do NOT require '!' as %21 — Android Uri often leaves it unencoded in queries.
        // If you want to allow either form, you could write:
        // assertTrue(qs.contains("%21") || qs.contains("!"))
    }

    @Test
    fun base_with_port_and_fragment_preserved()
    {
        val req = makeRequest(
            base = "https://example.com:8443/base#frag",
            paths = listOf("p"),
            query = mapOf("x" to "y")
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
    fun empty_path_and_query_keeps_base_intact()
    {
        val req = makeRequest("https://example.com/base")
        val url = req.toURL
        assertEquals("https://example.com/base", url.toString())
    }
}