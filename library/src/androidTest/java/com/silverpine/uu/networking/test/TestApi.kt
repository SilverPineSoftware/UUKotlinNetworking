package com.silverpine.uu.networking.test

import android.os.Parcelable
import com.silverpine.uu.core.uuFromJson
import com.silverpine.uu.core.uuToJson
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUHttpUri
import com.silverpine.uu.networking.UUJsonBody
import com.silverpine.uu.networking.UUQueryStringArgs
import com.silverpine.uu.networking.UURemoteApi
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class TestApiError(var errorCode: Int, var errorMessage: String): Parcelable
{
    override fun equals(other: Any?): Boolean
    {
        val o = (other as? TestApiError) ?: return false

        return (errorCode == o.errorCode &&
                errorMessage == o.errorMessage)
    }
}

@Serializable
data class TestApiObject(var id: String, var name: String, var data: String)
{
    override fun equals(other: Any?): Boolean
    {
        val o = (other as? TestApiObject) ?: return false

        return (id == o.id &&
                name == o.name &&
                data == o.data)
    }
}

interface ITestApi
{
    fun getObject(echo: TestApiObject?, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
    fun getList(count: Int, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)

    fun postObject(obj: TestApiObject, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
    fun postList(list: List<TestApiObject>, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>) -> Unit)

    /*
    fun postObject(request: TestApiObject, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
    fun postObjectWithQueryArgs(request: TestApiObject, query: UUQueryStringArgs, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
    fun postObjectWithPathArgs(request: TestApiObject, path: UUPathArgs, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
    fun postObjectWithPathAndQueryArgs(request: TestApiObject, path: UUPathArgs, query: UUQueryStringArgs, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)

    fun postList(request: List<TestApiObject>, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun postListWithQueryArgs(request: List<TestApiObject>, query: UUQueryStringArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun postListWithPathArgs(request: List<TestApiObject>, path: UUPathArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun postListWithPathAndQueryArgs(request: List<TestApiObject>, path: UUPathArgs, query: UUQueryStringArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)


    fun putObject(request: TestApiObject, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun putObjectWithQueryArgs(request: TestApiObject, query: UUQueryStringArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun putObjectWithPathArgs(request: TestApiObject, path: UUPathArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun putObjectWithPathAndQueryArgs(request: TestApiObject, path: UUPathArgs, query: UUQueryStringArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)

    fun putList(request: List<TestApiObject>, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun puListWithQueryArgs(request: List<TestApiObject>, query: UUQueryStringArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun puListWithPathArgs(request: List<TestApiObject>, path: UUPathArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)
    fun puListWithPathAndQueryArgs(request: List<TestApiObject>, path: UUPathArgs, query: UUQueryStringArgs, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>)->Unit)


    fun delete(completion: (UUHttpResponse<UUEmptyResponse, TestApiError>)->Unit)
    fun deleteWithQueryArgs(query: UUQueryStringArgs, completion: (UUHttpResponse<UUEmptyResponse, TestApiError>)->Unit)
    fun deleteWithPathArgs(path: UUPathArgs, completion: (UUHttpResponse<UUEmptyResponse, TestApiError>)->Unit)
    fun deleteWithPathAndQueryArgs(path: UUPathArgs, query: UUQueryStringArgs, completion: (UUHttpResponse<UUEmptyResponse, TestApiError>)->Unit)
*/
}


class TestApi(private val apiUrl: String): UURemoteApi<TestApiError>(UUHttpSession()), ITestApi
{
    init
    {
        session.logResponses = true
    }

    override fun getObject(echo: TestApiObject?, completion: (UUHttpResponse<TestApiObject, TestApiError>) -> Unit)
    {
        val queryArgs = UUQueryStringArgs()

        echo?.let()
        {
            if (it.id.isNotEmpty())
            {
                queryArgs["id"] = it.id
            }

            if (it.name.isNotEmpty())
            {
                queryArgs["name"] = it.name
            }

            if (it.data.isNotEmpty())
            {
                queryArgs["data"] = it.data
            }
        }

        val uri = UUHttpUri("$apiUrl/single", queryArgs)
        val request = UUHttpRequest<TestApiObject, TestApiError>(uri)
        internalExecute(request, completion)
    }

    override fun getList(count: Int, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>) -> Unit)
    {
        val queryArgs = UUQueryStringArgs()
        queryArgs["count"] = "$count"

        val uri = UUHttpUri("$apiUrl/multiple", query = queryArgs)
        val request = UUHttpRequest<List<TestApiObject>, TestApiError>(uri)
        internalExecute(request, completion)
    }

    override fun postObject(obj: TestApiObject, completion: (UUHttpResponse<TestApiObject, TestApiError>) -> Unit)
    {
        val uri = UUHttpUri("$apiUrl/single")
        val request = UUHttpRequest<TestApiObject, TestApiError>(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(obj, TestApiObject.serializer())
        internalExecute(request, completion)
    }

    override fun postList(list: List<TestApiObject>, completion: (UUHttpResponse<List<TestApiObject>, TestApiError>) -> Unit)
    {
        val uri = UUHttpUri("$apiUrl/single")
        val request = UUHttpRequest<List<TestApiObject>, TestApiError>(uri)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody<List<TestApiObject>>(list.uuToJson())

        internalExecute(request, completion)
    }

    private inline fun <reified ResponseType> internalExecute(
        request: UUHttpRequest<ResponseType, TestApiError>,
        noinline completion: (UUHttpResponse<ResponseType, TestApiError>) -> Unit)
    {
        prepareRequest(request)
        executeRequest(request, completion)
    }

    private inline fun <reified ResponseType, reified ErrorType> prepareRequest(request: UUHttpRequest<ResponseType, ErrorType>)
    {
        UULog.d(javaClass, "prepareRequest", "Preparing Request: ${request.uri}")
        request.responseParser = this::parseSuccess
        request.errorParser = this::parseError
    }

    private inline fun <reified ResponseType> parseSuccess(data: ByteArray, contentType: String, contentEncoding: String): ResponseType?
    {
        return data.uuFromJson()
    }

    private inline fun <reified ErrorType> parseError(data: ByteArray, contentType: String, contentEncoding: String, httpCode: Int): ErrorType?
    {
        return data.uuFromJson()
    }
}