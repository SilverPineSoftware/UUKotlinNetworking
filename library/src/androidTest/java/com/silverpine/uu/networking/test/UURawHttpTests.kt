package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.core.uuUnzip
import com.silverpine.uu.logging.UUConsoleLogger
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.networking.UUHttpStreamParser
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUHttpUri
import com.silverpine.uu.networking.UUJsonBody
import com.silverpine.uu.test.uuRandomLetters
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import kotlin.io.path.absolutePathString

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UURawHttpTests
{
    @OptIn(ExperimentalSerializationApi::class)
    @Before
    fun doBefore()
    {
        UULog.init(UUConsoleLogger())

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
        val request = UUHttpRequest(uri)
        val session = UUHttpSession()

        val latch = CountDownLatch(1)

        var response: UUHttpResponse? = null
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

        val request = UUHttpRequest(uri)

        request.method = UUHttpMethod.POST

        val count = 3
        request.headers.putSingle("uu-return-object-count", "$count")

        val session = UUHttpSession()

        request.responseHandler.successParser = UUHttpStreamParser { stream, response ->
            UUJson.fromStream(stream, Array<TestModel>::class.java)
        }

        val latch = CountDownLatch(1)

        var response: UUHttpResponse? = null
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.parsedResponse)
        //assert(response?.parsedResponse is Array<*> && response.parsedResponse.isArrayOf<TestModel>()<TestModel>())
        //Assert.assertTrue(response?.parsedResponse is Array<*> && response.parsedResponse.isArrayOf<TestModel>())
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

        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(model)

        request.responseHandler.successParser = UUHttpStreamParser { stream, response ->
            UUJson.fromStream(stream, TestModel::class.java)
        }

        val latch = CountDownLatch(1)

        val session = UUHttpSession()

        var response: UUHttpResponse? = null
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.parsedResponse)
        Assert.assertTrue(response?.parsedResponse is TestModel)
    }

    @Test
    fun test_0003_download_zip()
    {
        val uri = UUHttpUri("https://spsw.io/uu/random_zip_100.zip")

        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.GET

        request.responseHandler.successParser = UUHttpStreamParser { stream, response ->

            val applicationContext = InstrumentationRegistry.getInstrumentation().targetContext
            val outputFolder = Paths.get("${applicationContext.noBackupFilesDir}/uu2")
            stream.uuUnzip(outputFolder)
            outputFolder
        }

        val latch = CountDownLatch(1)

        val session = UUHttpSession()

        var response: UUHttpResponse? = null
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.parsedResponse)
        Assert.assertTrue(response?.parsedResponse is Path)

        val path = response?.parsedResponse as? Path
        Files.walk(path)
            .forEach()
            {
                UULog.d(javaClass, "printZip", it.absolutePathString())
            }
    }

    // sample how to use basic auth and client certs
    
    /*
    @Test
    fun test_0004_get_with_client_cert()
    {
        UULog.init(UUConsoleLogger())

        val url = "url here"
        val user = "user name here"
        val pwd = "password here"

        val cert = """
            -----BEGIN CERTIFICATE-----
            cert bytes
            -----END CERTIFICATE-----
            ....
            -----BEGIN PRIVATE KEY-----
            pk bytes
            -----END PRIVATE KEY-----
        """.trimIndent()

        val socketFactory = UUClientCertSocketFactory()
        socketFactory.plainTextPemCert = cert

        val uri = UUHttpUri(url)

        val request = UUHttpRequest<UUEmptyResponse, UUEmptyResponse>(uri)
        request.method = UUHttpMethod.GET
        request.socketFactory = socketFactory.getSocketFactory()

        val authProvider = object: UUBasicAuthorizationProvider
        {
            override val userName: String = user
            override val password: String = pwd
        }

        authProvider.attachAuthorization(request.headers)

        val latch = CountDownLatch(1)

        var response: UUHttpResponse<UUEmptyResponse,UUEmptyResponse>? = null
        val session = UUHttpSession<UUEmptyResponse>()
        session.logResponses = true
        session.executeRequest(request)
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
    }*/
}
