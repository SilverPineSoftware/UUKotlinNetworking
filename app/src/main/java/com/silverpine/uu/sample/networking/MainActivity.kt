package com.silverpine.uu.sample.networking

import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.networking.UUEmptyResponse
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUHttpUri
import com.silverpine.uu.networking.UUJsonBody
import kotlinx.serialization.Serializable

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        val request = UUHttpRequest<TestModel, UUEmptyResponse>(uri)
        request.method = UUHttpMethod.POST
        request.body = body

        request.responseParser =
            { data, _, _ ->
                UUJson.fromBytes(data, TestModel::class.java)
            }

        //val latch = CountDownLatch(1)

        var response: UUHttpResponse<TestModel, UUEmptyResponse>? = null
        val session = UUHttpSession<UUEmptyResponse>()
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