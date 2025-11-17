package com.silverpine.uu.networking.test

import androidx.annotation.Keep
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.networking.UUHttpHeader
import com.silverpine.uu.networking.UUHttpLoggingMode
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUJsonBody
import com.silverpine.uu.networking.UUTypedResponseHandler
import com.silverpine.uu.test.UUAssert
import com.silverpine.uu.test.uuRandomLetters
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val LOG_TAG = "UUHttpSessionTests"

@Keep
@Serializable
class GetModel
{
    var id: String = ""
    var name: String = ""
    var data: String = ""

    override fun toString(): String
    {
        return "id: $id, name: $name, data: $data"
    }
}

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UUHttpSessionTests
{
    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun doBefore()
    {
        TestLogger.init()

        UUJson.init(
            UUKotlinXJsonProvider(Json()
            {
                ignoreUnknownKeys = true
                namingStrategy = JsonNamingStrategy.SnakeCase
                explicitNulls = false
                encodeDefaults = true
                isLenient = true
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
    fun test_objectSerialization()
    {
        val obj = TestApiObject("1234", "UnitTest", "Data here")
        val json = UUJson.toJson(obj, obj.javaClass)
        UULog.debug(LOG_TAG, "test_objectSerialization, JSON: $json")
    }

    @Test
    fun test_errorSerialization()
    {
        val obj = TestApiError(1234, "UnitTest")
        val json = UUJson.toJson(obj, obj.javaClass)
        UULog.debug(LOG_TAG, "test_errorSerialization, JSON: $json")
    }

    @Test
    fun test_modelSerialization()
    {
        val obj = TestModel().apply { id = "1234"; name = "Unit Test"; level = 57; xp = 99; }
        val json = UUJson.toJson(obj, obj.javaClass)
        UULog.debug(LOG_TAG, "test_errorSerialization, JSON: $json")
    }

    @Test
    fun test_0000_simple_get()
    {
        val uri = "https://spsw.io/uu/echo_json.php"
        val request = UUHttpRequest(uri)
        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0000_simple_get, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0001_get_list()
    {
        val uri = "https://spsw.io/uu/echo_json.php?id=foo&name=bar&level=1&xp=57"

        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.GET
        request.loggingMode = UUHttpLoggingMode.Verbose

        val count = 3
        request.headers.putSingle("uu-return-object-count", "$count")

        request.responseHandler = UUTypedResponseHandler<Array<TestModel>, Void>(Array<TestModel>::class.java, Void::class.java)

        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is Array<*> && success.isArrayOf<TestModel>())
        UULog.debug(LOG_TAG, "test_0001_get_list, Success: $success")
    }

    @Test
    fun test_0002_simple_echo_post()
    {
        val uri = "https://spsw.io/uu/echo_json_post.php"

        val model = TestModel()
        model.id = UURandom.uuid()
        model.name = uuRandomLetters(10)
        model.level = UURandom.uByte().toInt()
        model.xp = UURandom.uShort().toInt()

        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(model)
        request.loggingMode = UUHttpLoggingMode.Verbose

        request.responseHandler = UUTypedResponseHandler<TestModel, Void>(TestModel::class.java, Void::class.java)

        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is TestModel)
        UULog.debug(LOG_TAG, "test_0002_simple_echo_post, Success: $success")
    }

    @Test
    fun test_0003_get_object()
    {
        val uri = "https://spsw.io/uu/get_object.php"
        val request = UUHttpRequest(uri)
        request.loggingMode = UUHttpLoggingMode.Verbose
        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0003_get_object, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0004_get_object_gzip()
    {
        val uri = "https://spsw.io/uu/get_object.php"
        val request = UUHttpRequest(uri)
        request.headers.put(UUHttpHeader.AcceptEncoding, "gzip")
        request.loggingMode = UUHttpLoggingMode.Verbose

        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0004_get_object_gzip, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0005_get_object_deflate()
    {
        val uri = "https://spsw.io/uu/get_object.php"
        val request = UUHttpRequest(uri)
        request.headers.put(UUHttpHeader.AcceptEncoding, "deflate")
        request.loggingMode = UUHttpLoggingMode.Verbose

        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0005_get_object_deflate, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0005_get_with_error()
    {
        val uri = "https://spsw.io/uu/echo_json.php?error=Failed&errorMessage=RequestFailed"
        val request = UUHttpRequest(uri)
        request.headers.putSingle("uu-status-code", "400")
        request.loggingMode = UUHttpLoggingMode.Verbose
        val session = UUHttpSession()

        val response = doRequest(session, request)
        Assert.assertNotNull(response.error)
        assertEquals(400, response.httpStatusCode)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0005_get_with_error, Error: ${bytes.uuUtf8().getOrNull()}")
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun doRequest(session: UUHttpSession, request: UUHttpRequest, timeout: Long = UUDate.Constants.MILLIS_IN_ONE_SECOND * 30): UUHttpResponse
    {
        val latch = CountDownLatch(1)
        val responseContainer = AtomicReference<UUHttpResponse?>(null)

        session.executeRequest(request)
        { response ->
            responseContainer.store(response)
            latch.countDown()
        }

        latch.await(timeout, TimeUnit.MILLISECONDS)
        val response = UUAssert.unwrap(responseContainer.load())
        return response
    }
}
