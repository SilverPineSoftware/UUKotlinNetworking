package com.silverpine.uu.networking.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.core.uuSleep
import com.silverpine.uu.core.uuUnzip
import com.silverpine.uu.logging.UULog
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.io.path.absolutePathString

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UURawHttpTests
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
        val request = UURawHttpRequest(uri)

        val latch = CountDownLatch(1)

        var response: UURawHttpResponse? = null
        request.execute()
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

        val request = UURawHttpRequest(uri)
        request.method = UUHttpMethod.POST

        val count = 3
        request.headers.putSingle("uu-return-object-count", "$count")

        request.successResponseHandler =
        {
            UUJson.fromStream(it, Array<TestModel>::class.java)
        }

        val latch = CountDownLatch(1)

        var response: UURawHttpResponse? = null
        request.execute()
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.success)
        //Assert.assertTrue(response?.success is Array<TestModel>)
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

        val request = UURawHttpRequest(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(model)

        request.successResponseHandler =
        { it ->
            UUJson.fromStream(it, TestModel::class.java)
        }

        val latch = CountDownLatch(1)

        var response: UURawHttpResponse? = null
        request.execute()
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.success)
        Assert.assertTrue(response?.success is TestModel)
    }

    @Test
    fun test_0003_download_zip()
    {
        val uri = UUHttpUri("https://spsw.io/uu/random_zip_100.zip")

        val request = UURawHttpRequest(uri)
        request.method = UUHttpMethod.GET

        request.successResponseHandler =
        { it ->
            val applicationContext = InstrumentationRegistry.getInstrumentation().targetContext
            val outputFolder = Paths.get("${applicationContext.noBackupFilesDir}/uu2")
            it.uuUnzip(outputFolder)
            outputFolder
        }

        val latch = CountDownLatch(1)

        var response: UURawHttpResponse? = null
        request.execute()
        {
            response = it
            latch.countDown()
        }

        latch.await()

        Assert.assertNotNull(response)
        Assert.assertNotNull(response?.success)
        Assert.assertTrue(response?.success is Path)

        val path = response?.success as? Path
        Files.walk(path)
            .forEach()
            {
                UULog.d(javaClass, "printZip", it.absolutePathString())
            }
    }
}
