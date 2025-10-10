package com.silverpine.uu.sample.networking

import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.logging.UUConsoleLogger
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.networking.UUHttpStreamParser
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUHttpUri
import com.silverpine.uu.networking.UUJsonBody
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

class MainActivity : AppCompatActivity()
{
    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UULog.init(UUConsoleLogger())

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

        test_0001_simple_echo_post()
    }

    fun test_0001_simple_echo_post()
    {
        val uri = UUHttpUri("https://spsw.io/uu/echo_json_post.php")

        val model = TestModel()
        model.id = UURandom.uuid()
        model.name = "hello"
        model.level = UURandom.uByte().toInt()
        model.xp = UURandom.uShort().toInt()

        val body = UUJsonBody(model)
        val request = UUHttpRequest(uri)
        request.method = UUHttpMethod.POST
        request.body = body

        request.responseHandler.successParser = UUHttpStreamParser { stream, response ->
            UUJson.fromStream(stream, TestModel::class.java)
        }

        //val latch = CountDownLatch(1)

        var response: UUHttpResponse? = null
        val session = UUHttpSession()
        session.executeRequest(request)
        {
            response = it
            //latch.countDown()
        }

        //latch.await()

//        Assert.assertNotNull(response)
//        Assert.assertNotNull(response?.parsedResponse)
//        Assert.assertTrue(response?.parsedResponse is TestModel)
    }
}

@Keep
@Serializable
class TestModel()
{
    var id: String = ""
    var name: String = ""
    var level: Int = 0
    var xp: Int = 0
}