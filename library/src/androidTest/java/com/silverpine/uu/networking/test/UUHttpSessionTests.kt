package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUJsonBody
import com.silverpine.uu.networking.handlers.UUTypedResponseHandler
import com.silverpine.uu.test.UUAssert
import com.silverpine.uu.test.instrumented.annotations.UUIntegrationTest
import com.silverpine.uu.test.uuRandomLetters
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
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

private const val LOG_TAG = "UUHttpSessionTests"

/*
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
}*/

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@UUIntegrationTest
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
    fun test_0000_simple_get() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/echo_json.php"
        val request = UUHttpRequest(uri)
        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0000_simple_get, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0001_get_list() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/echo_json.php?id=foo&name=bar&level=1&xp=57"

        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.GET
        request.loggingMode = UUHttpLoggingMode.Verbose

        val count = 3
        request.headers.putSingle("uu-return-object-count", "$count")

        request.responseHandler = UUTypedResponseHandler(Array<TestModel>::class.java, Void::class.java)

        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is Array<*> && success.isArrayOf<TestModel>())
        UULog.debug(LOG_TAG, "test_0001_get_list, Success: $success")
    }

    @Test
    fun test_0002_simple_echo_post() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/echo_json_post.php"

        val model = TestModel()
        model.id = UURandom.uuid()
        model.name = uuRandomLetters(10)
        model.level = UURandom.uByte().toInt()
        model.xp = UURandom.uShort().toInt()

        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(model)
        request.loggingMode = UUHttpLoggingMode.Verbose

        request.responseHandler = UUTypedResponseHandler(TestModel::class.java, Void::class.java)

        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is TestModel)
        UULog.debug(LOG_TAG, "test_0002_simple_echo_post, Success: $success")
    }

    @Test
    fun test_0003_get_object() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/get_object.php"
        val request = UUHttpRequest(uri)
        request.loggingMode = UUHttpLoggingMode.Verbose
        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0003_get_object, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0004_get_object_gzip() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/get_object.php"
        val request = UUHttpRequest(uri)
        request.headers.put(UUHttpHeader.AcceptEncoding, "gzip")
        request.loggingMode = UUHttpLoggingMode.Verbose

        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0004_get_object_gzip, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0005_get_object_deflate() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/get_object.php"
        val request = UUHttpRequest(uri)
        request.headers.put(UUHttpHeader.AcceptEncoding, "deflate")
        request.loggingMode = UUHttpLoggingMode.Verbose

        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNull(response.error)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0005_get_object_deflate, Success: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0005_get_with_error() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/echo_json.php?error=Failed&errorMessage=RequestFailed"
        val request = UUHttpRequest(uri)
        request.headers.putSingle("uu-status-code", "400")
        request.loggingMode = UUHttpLoggingMode.Verbose
        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNotNull(response.error)
        assertEquals(400, response.httpStatusCode)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0005_get_with_error, Error: ${bytes.uuUtf8().getOrNull()}")
    }

    @Test
    fun test_0006_get_with_nullable_response_no_data() = runBlocking()
    {
        val uri = "${TestConfig.BASE_URL}/echo_json.php?error=Failed&errorMessage=RequestFailed"
        val request = UUHttpRequest(uri)
        request.headers.putSingle("uu-status-code", "400")
        request.loggingMode = UUHttpLoggingMode.Verbose
        val session = UUHttpSession()

        val response = session.execute(request)
        Assert.assertNotNull(response.error)
        assertEquals(400, response.httpStatusCode)

        val success = UUAssert.unwrap(response.parsedResponse)
        assert(success is ByteArray)
        val bytes = UUAssert.unwrap(success as? ByteArray)
        UULog.debug(LOG_TAG, "test_0005_get_with_error, Error: ${bytes.uuUtf8().getOrNull()}")
    }
}
