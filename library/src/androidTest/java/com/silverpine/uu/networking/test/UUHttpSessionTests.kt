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
        uuSleep("doAfter", 100)
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

    //https://spsw.io/uu


    /*
    class UUNetworkingTestConfig
    {
        var testServerApiHost: String = ""
        var doesNotExistUrl: String = ""
        var uploadFilePath: URL? = nil

        required init(plistFile: String)
        {
            if let path = Bundle.module.url(forResource: plistFile, withExtension: "plist")
            {
                if let d = NSDictionary(contentsOf: path) as? [AnyHashable:Any]
                {
                    if let str = d["test_server_api_host"] as? String
                    {
                        testServerApiHost = str
                    }

                    if let str = d["does_not_exist_url"] as? String
                    {
                        doesNotExistUrl = str
                    }

                    if let fullFileName = d["upload_image_file_name"] as? String
                    {
                        let namePart = fullFileName.uuGetFileName()
                        let extPart = fullFileName.uuGetFileExtension()
                        let nameOnly = namePart.replacingOccurrences(of: ".\(extPart)", with: "")

                        if let path = Bundle.module.url(forResource: nameOnly, withExtension: extPart)
                        {
                            uploadFilePath = path
                        }
                    }
                }
            }
        }

        var timeoutUrl: String
        {
            return "\(testServerApiHost)/timeout.php"
        }

        var echoJsonUrl: String
        {
            return "\(testServerApiHost)/echo_json.php"
        }

        var invalidJsonUrl: String
        {
            return "\(testServerApiHost)/invalid_json.php"
        }

        var redirectUrl: String
        {
            return "\(testServerApiHost)/redirect.php"
        }

        var formPostUrl: String
        {
            return "\(testServerApiHost)/form.php"
        }

        var downloadFileUrl: String
        {
            return "\(testServerApiHost)/download.php"
        }
    }


    func UULoadNetworkingTestConfig() -> UUNetworkingTestConfig
    {
        let cfg = UUNetworkingTestConfig(plistFile: "UUNetworkingTestConfig")

        XCTAssertFalse(cfg.testServerApiHost.isEmpty, "Expected a valid test server api host")
        XCTAssertFalse(cfg.doesNotExistUrl.isEmpty, "Expected a valid does not exist url")

        return cfg
    }
*/

    /*
    func test_getCodableObject()
    {
        let exp = uuExpectationForMethod()

        let cfg = UULoadNetworkingTestConfig()
        let url = cfg.echoJsonUrl

        var queryArgs = UUQueryStringArgs()
        queryArgs["fieldOne"] = "SomeValue"
        queryArgs["fieldTwo"] = 1234

        var headers = UUHttpHeaders()
        headers["UU-Return-Object-Count"] = 1

        UUHttpSession.get(url: url, queryArguments: queryArgs, headers: headers)
        { (response: SimpleObject?, err: Error?) in

            XCTAssertNotNil(response)
            XCTAssertNil(err)
            exp.fulfill()
        }

        uuWaitForExpectations()
    }*/
}


//@Keep
//@JsonClass(generateAdapter = true)
//open class TestModel
//{
//    @Json
//    var id: String = ""
//
//    @Json
//    var name: String = ""
//
//    @Json
//    var level: Int = 0
//
//    @Json
//    var xp: Int = 0
//}