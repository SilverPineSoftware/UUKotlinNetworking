package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.logging.UUConsoleLogWriter
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.UULogger
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.uuIsHttpSuccess
import com.silverpine.uu.test.UUAssert
import com.silverpine.uu.test.instrumented.annotations.UUIntegrationTest
import kotlinx.serialization.ExperimentalSerializationApi
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
import java.util.concurrent.atomic.AtomicReference

private const val LOG_TAG = "UURemoteApiTests"

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@UUIntegrationTest
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

    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun doBefore()
    {
        TestLogger.init()

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

        var responseContainer = AtomicReference<UUResult<TestApiObject>>()

        api.getObject(null)
        {
            responseContainer.set(it)
            latch.countDown()
        }

        latch.await()

        val response = UUAssert.unwrap(responseContainer.get())
        val success = UUAssert.unwrap(response.getOrNull())
        UULog.debug(LOG_TAG, "test_0000_getObject, Response: $success")
        //assertReply(200, DEFAULT_API_OBJECT, null, response)
    }

    @Test
    fun test_0002_getObjectWithOverrides()
    {
        val latch = CountDownLatch(1)

        var responseContainer = AtomicReference<UUResult<TestApiObject>>()

        val override = TestApiObject("one", "two", "three")

        api.getObject(override)
        {
            responseContainer.set(it)
            latch.countDown()
        }

        latch.await()

        val response = UUAssert.unwrap(responseContainer.get())
        val success = UUAssert.unwrap(response.getOrNull())
        UULog.debug(LOG_TAG, "test_0002_getObjectWithOverrides, Response: $success")

        //assertReply(200, override, null, response)
    }

    @Test
    fun test_0003_getArray()
    {
        val latch = CountDownLatch(1)

        var responseContainer = AtomicReference<UUResult<Array<TestApiObject>>>()

        val count = 7
        api.getArray(count)
        {
            responseContainer.set(it)
            latch.countDown()
        }

        latch.await()

        val response = UUAssert.unwrap(responseContainer.get())
        val success = UUAssert.unwrap(response.getOrNull())
        UULog.debug(LOG_TAG, "test_0003_getArray, Response: $success")

        // assertArrayReply(200, count, null, response)

    }

    @Test
    fun test_0004_postObject()
    {
        val latch = CountDownLatch(1)

        var responseContainer = AtomicReference<UUResult<TestApiObject>>()

        val post = TestApiObject("one", "two", "three")
        api.postObject(post)
        {
            responseContainer.set(it)
            latch.countDown()
        }

        latch.await()

        val response = UUAssert.unwrap(responseContainer.get())
        val success = UUAssert.unwrap(response.getOrNull())
        UULog.debug(LOG_TAG, "test_0004_postObject, Response: $success")
        //assertReply(200, DEFAULT_API_OBJECT, null, response)
    }

    @Test
    fun test_0005_postList()
    {
        val latch = CountDownLatch(1)

        var responseContainer = AtomicReference<UUResult<Array<TestApiObject>>>()

        val post = arrayOf(
            TestApiObject("one", "two", "three"),
            TestApiObject("A", "B", "C"),
            TestApiObject("Foo", "Bar", "Baz"))

        api.postArray(post)
        {
            responseContainer.set(it)
            latch.countDown()
        }

        latch.await()

        val response = UUAssert.unwrap(responseContainer.get())
        val success = UUAssert.unwrap(response.getOrNull())
        UULog.debug(LOG_TAG, "test_0005_postList, Response: $success")

        //assertArrayReply(200, post.size, null, response)
    }


    private fun assertReply(
        expectedHttpCode: Int,
        expectedSuccess: TestApiObject?,
        expectedError: TestApiError?,
        response: UUHttpResponse?) //<TestApiObject, TestApiError>?)
    {
        Assert.assertNotNull(response)
        Assert.assertEquals(expectedHttpCode, response?.httpStatusCode)

        if (expectedSuccess != null)
        {
            Assert.assertNotNull(response?.parsedResponse)
            Assert.assertEquals(expectedSuccess, response?.parsedResponse)
        }
        else
        {
            Assert.assertNull(response?.parsedResponse)
        }

        if (expectedError != null)
        {
            Assert.assertNotNull(response?.error)

            //val apiError = response?.error?.userInfo?.getParcelable(UUHttpError.USER_INFO_KEY, TestApiError::class.java)
            //Assert.assertNotNull(apiError)
            //Assert.assertEquals(expectedError, apiError)
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
        response: UUHttpResponse?)
    {
        Assert.assertNotNull(response)
        Assert.assertEquals(expectedHttpCode, response?.httpStatusCode)

        if (expectedHttpCode.uuIsHttpSuccess())
        {
            Assert.assertNotNull(response?.parsedResponse)
            //Assert.assertEquals(expectedResponseCount, response?.success?.size)
        }
        else
        {
            Assert.assertNull(response?.parsedResponse)
        }

        if (expectedError != null)
        {
            Assert.assertNotNull(response?.error)

            //val apiError = response?.error?.userInfo?.getParcelable(UUHttpError.USER_INFO_KEY, TestApiError::class.java)
            //Assert.assertNotNull(apiError)
            //Assert.assertEquals(expectedError, apiError)
        }
        else
        {
            Assert.assertNull(response?.error)
        }
    }
}