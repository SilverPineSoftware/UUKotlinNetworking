package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.networking.UUHttpUri
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UUHttpUriTests
{
    companion object
    {
        const val ROOT_URL = "https://foobar.unit.test"
    }

    @Test
    fun test_0000_formatUrl_empty()
    {
        val input = UUHttpUri("")
        val actual = input.toURL()?.toString()
        Assert.assertNull(actual)
    }

    @Test
    fun test_0001_formatUrl_noQuery_noPath()
    {
        val input = UUHttpUri(ROOT_URL)
        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals(ROOT_URL, actual)
    }

    @Test
    fun test_0002_formatUrl_singleQuery_noPath()
    {
        val input = UUHttpUri(ROOT_URL)
        input.query["foo"] = "bar"

        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("$ROOT_URL?foo=bar", actual)
    }

    @Test
    fun test_0003_formatUrl_doubleQuery_noPath()
    {
        val input = UUHttpUri(ROOT_URL)
        input.query["foo"] = "bar"
        input.query["two"] = "three"

        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("$ROOT_URL?foo=bar&two=three", actual)
    }

    @Test
    fun test_0004_formatUrl_noQuery_singlePath()
    {
        val input = UUHttpUri(ROOT_URL)
        input.path.add("one")

        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("$ROOT_URL/one", actual)
    }

    @Test
    fun test_0005_formatUrl_noQuery_doublePath()
    {
        val input = UUHttpUri(ROOT_URL)
        input.path.add("one")
        input.path.add("two")

        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("$ROOT_URL/one/two", actual)
    }

    @Test
    fun test_0006_formatUrl_singleQuery_singlePath()
    {
        val input = UUHttpUri(ROOT_URL)
        input.query["foo"] = "bar"
        input.path.add("one")

        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("$ROOT_URL/one?foo=bar", actual)
    }

    @Test
    fun test_0006_formatUrl_doubleQuery_doublePath()
    {
        val input = UUHttpUri(ROOT_URL)
        input.query["foo"] = "bar"
        input.query["baz"] = "what"
        input.path.add("one")
        input.path.add("two")

        val actual = input.toURL()?.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("$ROOT_URL/one/two?foo=bar&baz=what", actual)
    }
}