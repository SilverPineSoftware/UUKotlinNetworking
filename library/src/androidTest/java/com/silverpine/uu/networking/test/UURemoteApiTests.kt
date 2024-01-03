package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.networking.UUHttpError
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.uuIsHttpSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UURemoteApiTests
{
    private val api: ITestApi = TestApi(TestConfig.ECHO_SERVER_URL)

    companion object
    {
        private const val DEFAULT_ID = "12345"
        private const val DEFAULT_NAME = "IntegrationTest"
        private const val DEFAULT_DATA = "This is for live integration testing between mobile libraries and real servers."

        val DEFAULT_API_OBJECT = TestApiObject(DEFAULT_ID, DEFAULT_NAME, DEFAULT_DATA)
    }

    @Before
    fun doBefore()
    {
        UUJson.init(UUKotlinXJsonProvider(Json()
        {
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        })
        )
    }

    @After
    fun doAfter()
    {
        UUJson.init(UUKotlinXJsonProvider(Json.Default))

        // A single shot timer gets cleaned up after the timer block is invoked, so we need
        // to wait.  This is a hacky way to test, but works in this simple case
        uuSleep(100)
    }

    @Test
    fun test_0000_getObject()
    {
        val latch = CountDownLatch(1)

        var response: UUHttpResponse<TestApiObject, TestApiError>? = null

        api.getObject(null)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        assertReply(200, DEFAULT_API_OBJECT, null, response)
    }

    @Test
    fun test_0002_getObjectWithOverrides()
    {
        val latch = CountDownLatch(1)

        var response: UUHttpResponse<TestApiObject, TestApiError>? = null

        val override = TestApiObject("one", "two", "three")

        api.getObject(override)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        assertReply(200, override, null, response)
    }

    @Test
    fun test_0003_getArray()
    {
        val latch = CountDownLatch(1)

        var response: UUHttpResponse<Array<TestApiObject>, TestApiError>? = null

        val count = 7
        api.getArray(count)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        assertArrayReply(200, count, null, response)
    }

    @Test
    fun test_0004_postObject()
    {
        val latch = CountDownLatch(1)

        var response: UUHttpResponse<TestApiObject, TestApiError>? = null

        val post = TestApiObject("one", "two", "three")

        api.postObject(post)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        assertReply(200, post, null, response)
    }

    @Test
    fun test_0005_postList()
    {
        val latch = CountDownLatch(1)

        var response: UUHttpResponse<Array<TestApiObject>, TestApiError>? = null

        val post = arrayOf(
            TestApiObject("one", "two", "three"),
            TestApiObject("A", "B", "C"),
            TestApiObject("Foo", "Bar", "Baz"))

        api.postArray(post)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        assertArrayReply(200, post.size, null, response)
    }


    private fun assertReply(
        expectedHttpCode: Int,
        expectedSuccess: TestApiObject?,
        expectedError: TestApiError?,
        response: UUHttpResponse<TestApiObject, TestApiError>?)
    {
        Assert.assertNotNull(response)
        Assert.assertEquals(expectedHttpCode, response?.httpCode)

        if (expectedSuccess != null)
        {
            Assert.assertNotNull(response?.success)
            Assert.assertEquals(expectedSuccess, response?.success)
        }
        else
        {
            Assert.assertNull(response?.success)
        }

        if (expectedError != null)
        {
            Assert.assertNotNull(response?.error)

            val apiError = response?.error?.userInfo?.getParcelable(UUHttpError.USER_INFO_KEY, TestApiError::class.java)
            Assert.assertNotNull(apiError)
            Assert.assertEquals(expectedError, apiError)
        }
        else
        {
            Assert.assertNull(response?.error)
        }
    }

    private fun assertArrayReply(
        expectedHttpCode: Int,
        expectedResponseCount: Int,
        expectedError: TestApiError?,
        response: UUHttpResponse<Array<TestApiObject>, TestApiError>?)
    {
        Assert.assertNotNull(response)
        Assert.assertEquals(expectedHttpCode, response?.httpCode)

        if (expectedHttpCode.uuIsHttpSuccess())
        {
            Assert.assertNotNull(response?.success)
            Assert.assertEquals(expectedResponseCount, response?.success?.size)
        }
        else
        {
            Assert.assertNull(response?.success)
        }

        if (expectedError != null)
        {
            Assert.assertNotNull(response?.error)

            val apiError = response?.error?.userInfo?.getParcelable(UUHttpError.USER_INFO_KEY, TestApiError::class.java)
            Assert.assertNotNull(apiError)
            Assert.assertEquals(expectedError, apiError)
        }
        else
        {
            Assert.assertNull(response?.error)
        }
    }
}