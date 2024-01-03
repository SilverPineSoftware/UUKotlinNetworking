package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.networking.*
import com.silverpine.uu.test.uuRandomLetters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.*
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UUHttpSessionTests
{
    @Before
    fun doBefore()
    {
        UUJson.init(
            UUKotlinXJsonProvider(Json()
        {
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        })
        )
    }

    @After
    fun doAfter()
    {
        UUJson.init(UUKotlinXJsonProvider(Json))

        // A single shot timer gets cleaned up after the timer block is invoked, so we need
        // to wait.  This is a hacky way to test, but works in this simple case
        uuSleep(100)
    }

    @Test
    fun test_0000_simple_get()
    {
        val uri = UUHttpUri("https://spsw.io/uu/echo_json.php")
        val request = UUHttpRequest<UUEmptyResponse, UUEmptyResponse>(uri)

        val latch = CountDownLatch(1)

        var response: UUHttpResponse<UUEmptyResponse, UUEmptyResponse>? = null
        val session = UUHttpSession<UUEmptyResponse>()
        session.logResponses = true
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
    }

    @Test
    fun test_0001_get_list()
    {
        val uri = UUHttpUri("https://spsw.io/uu/echo_json.php?id=foo&name=bar&level=1&xp=57")

        val request = UUHttpRequest<Array<TestModel>, UUEmptyResponse>(uri)
        request.method = UUHttpMethod.POST

        val count = 3
        request.headers.putSingle("uu-return-object-count", "$count")

        request.responseParser =
            { bytes, contentType, contentEncoding ->

                UUJson.fromBytes(bytes, Array<TestModel>::class.java)
            }

        val latch = CountDownLatch(1)

        var response: UUHttpResponse<Array<TestModel>, UUEmptyResponse>? = null
        val session = UUHttpSession<UUEmptyResponse>()
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.success)
        Assert.assertTrue(response?.success is Array<TestModel>)
    }

    @Test
    fun test_0002_simple_echo_post()
    {
        val uri = UUHttpUri("https://spsw.io/uu/echo_json_post.php")

        val model = TestModel()
        model.id = UURandom.uuid()
        model.name = uuRandomLetters(10)
        model.level = UURandom.uByte().toInt()
        model.xp = UURandom.uShort().toInt()

        val request = UUHttpRequest<TestModel, UUEmptyResponse>(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(model)

        request.responseParser =
        { bytes, contentType, contentEncoding ->
            UUJson.fromBytes(bytes, TestModel::class.java)
        }

        val latch = CountDownLatch(1)

        var response: UUHttpResponse<TestModel, UUEmptyResponse>? = null
        val session = UUHttpSession<UUEmptyResponse>()
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.success)
        Assert.assertTrue(response?.success is TestModel)
    }
}
