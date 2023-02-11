package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.networking.UUQueryStringArgs
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UUQueryStringArgsTests
{
    @Test
    fun test_0000_toString_empty()
    {
        val input = UUQueryStringArgs()
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("", actual)
    }

    @Test
    fun test_0001_toString_single()
    {
        val input = UUQueryStringArgs()
        input["foo"] = "bar"
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("?foo=bar", actual)
    }

    @Test
    fun test_0002_toString_double()
    {
        val input = UUQueryStringArgs()
        input["foo"] = "bar"
        input["baz"] = "red"
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("?foo=bar&baz=red", actual)
    }
}