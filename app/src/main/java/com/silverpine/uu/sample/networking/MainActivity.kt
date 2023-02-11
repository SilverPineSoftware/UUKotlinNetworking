package com.silverpine.uu.sample.networking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.Keep
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UURandom
import com.silverpine.uu.networking.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UUJson.init(Moshi.Builder().build())
        test_0001_simple_echo_post()
    }

    fun test_0001_simple_echo_post()
    {
        val url = "https://spsw.io/uu/echo_json_post.php"

        val model = TestModel()
        model.id = UURandom.uuid()
        model.name = "hello"
        model.level = UURandom.uByte().toInt()
        model.xp = UURandom.uShort().toInt()

        val body = UUJsonBody(model)
        val request = UUTypedHttpRequest<TestModel, UUEmptyResponse>(url, method = UUHttpMethod.POST, body = body)
        request.responseParser = UUTypedJsonDataParser(TestModel::class.java)

        //val latch = CountDownLatch(1)

        var response: UUTypedHttpResponse<TestModel, UUEmptyResponse>? = null
        val session = UUDefaultTypedHttpSession()
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
@JsonClass(generateAdapter = true)
open class TestModel
{
    @Json
    var id: String = ""

    @Json
    var name: String = ""

    @Json
    var level: Int = 0

    @Json
    var xp: Int = 0
}