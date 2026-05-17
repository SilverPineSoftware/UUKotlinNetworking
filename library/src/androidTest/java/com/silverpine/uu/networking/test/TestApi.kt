package com.silverpine.uu.networking.test

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUJsonBody
import com.silverpine.uu.networking.UUQueryStringsArgs
import com.silverpine.uu.networking.UURemoteApi
import com.silverpine.uu.networking.UUTypedHttpRequest
import kotlinx.serialization.Serializable

@Serializable
// @Parcelize
data class TestApiError(
    var errorCode: Int = 0,
    var errorMessage: String = "") //: Parcelable
{
    override fun equals(other: Any?): Boolean
    {
        val o = (other as? TestApiError) ?: return false

        return (errorCode == o.errorCode &&
                errorMessage == o.errorMessage)
    }

    override fun hashCode(): Int
    {
        var result = errorCode
        result = 31 * result + errorMessage.hashCode()
        return result
    }
}

@Serializable
data class TestApiObject(
    var id: String = "",
    var name: String = "",
    var data: String = "")
{
    override fun equals(other: Any?): Boolean
    {
        val o = (other as? TestApiObject) ?: return false

        return (id == o.id &&
                name == o.name &&
                data == o.data)
    }

    override fun hashCode(): Int
    {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

interface ITestApi
{
//    fun getObject(echo: TestApiObject?, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
//    fun getArray(count: Int, completion: (UUHttpResponse<Array<TestApiObject>, TestApiError>)->Unit)
//
//    fun postObject(obj: TestApiObject, completion: (UUHttpResponse<TestApiObject, TestApiError>)->Unit)
//    fun postArray(array: Array<TestApiObject>, completion: (UUHttpResponse<Array<TestApiObject>, TestApiError>) -> Unit)

    suspend fun getObject(echo: TestApiObject?): UUResult<TestApiObject, UUError>
    suspend fun getArray(count: Int): UUResult<Array<TestApiObject>, UUError>

    suspend fun postObject(obj: TestApiObject): UUResult<TestApiObject, UUError>
    suspend fun postArray(array: Array<TestApiObject>): UUResult<Array<TestApiObject>, UUError>

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


class TestApi(private val apiUrl: String): UURemoteApi(UUHttpSession()), ITestApi
{
    init
    {
        //session.logResponses = true
    }

    override suspend fun getObject(echo: TestApiObject?): UUResult<TestApiObject, UUError>
    {
        val queryArgs: UUQueryStringsArgs = hashMapOf()

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

        val request = UUTypedHttpRequest(
            url = "$apiUrl/single",
            query = queryArgs,
            successClass = TestApiObject::class.java,
            errorClass = TestApiError::class.java)

        val response = executeRequest(request)

        val result = response.parsedResponse as? TestApiObject
        return if (result != null)
        {
            UUResult.success(result)
        }
        else
        {
            UUResult.failure(response.error ?: UUError(code = -1))
        }
    }

    override suspend fun getArray(count: Int): UUResult<Array<TestApiObject>, UUError>
    {
        val queryArgs: UUQueryStringsArgs = hashMapOf()
        queryArgs["count"] = "$count"

        val request = UUTypedHttpRequest(
            url = "$apiUrl/multiple", query = queryArgs,
            successClass = Array<TestApiObject>::class.java,
            errorClass = TestApiError::class.java)

        val response = executeRequest(request)

        @Suppress("UNCHECKED_CAST")
        val result = (response.parsedResponse as? Array<*>) as? Array<TestApiObject>
        return if (result != null)
        {
            UUResult.success(result)
        }
        else
        {
            UUResult.failure(response.error ?: UUError(code = -1))
        }
    }

    override suspend fun postObject(obj: TestApiObject): UUResult<TestApiObject, UUError>
    {
        val request = UUTypedHttpRequest(
            url = "$apiUrl/single",
            successClass = TestApiObject::class.java,
            errorClass = TestApiError::class.java)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(obj)

        val response = executeRequest(request)

        val result = response.parsedResponse as? TestApiObject
        return if (result != null)
        {
            UUResult.success(result)
        }
        else
        {
            UUResult.failure(response.error ?: UUError(code = -1))
        }
    }

    override suspend fun postArray(array: Array<TestApiObject>): UUResult<Array<TestApiObject>, UUError>
    {
        val request = UUTypedHttpRequest(
            url = "$apiUrl/single",
            successClass = Array<TestApiObject>::class.java,
            errorClass = TestApiError::class.java)
        request.method = UUHttpMethod.POST
        request.body = UUJsonBody(array)

        val response = executeRequest(request)

        @Suppress("UNCHECKED_CAST")
        val result = (response.parsedResponse as? Array<*>) as? Array<TestApiObject>
        return if (result != null)
        {
            UUResult.success(result)
        }
        else
        {
            UUResult.failure(response.error ?: UUError(code = -1))
        }
    }
}