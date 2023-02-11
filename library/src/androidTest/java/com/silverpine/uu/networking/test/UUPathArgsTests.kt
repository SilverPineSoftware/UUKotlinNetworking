package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.networking.UUPathArgs
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UUPathArgsTests
{
    @Test
    fun test_0000_toString_empty()
    {
        val input = UUPathArgs()
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("", actual)
    }

    @Test
    fun test_0001_toString_single_constructor()
    {
        val input = UUPathArgs("foo")
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("/foo", actual)
    }

    @Test
    fun test_0002_toString_single_add()
    {
        val input = UUPathArgs("foo")
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("/foo", actual)
    }

    @Test
    fun test_0002_toString_double_constructor()
    {
        val input = UUPathArgs("foo", "bar")
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("/foo/bar", actual)
    }

    @Test
    fun test_0002_toString_double_add()
    {
        val input = UUPathArgs("foo", "bar")
        val actual = input.toString()
        Assert.assertNotNull(actual)
        Assert.assertEquals("/foo/bar", actual)
    }
}