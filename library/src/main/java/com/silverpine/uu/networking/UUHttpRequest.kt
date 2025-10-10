package com.silverpine.uu.networking

import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.networking.authorization.UUHttpAuthorizationProvider
import java.net.HttpURLConnection
import java.net.Proxy
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

//typealias UUByteArrayParser<T> = ((ByteArray,String,String)->T?)
//typealias UUErrorParser<T> = ((ByteArray,String,String,Int)->T?)

open class UUHttpRequest(var uri: UUHttpUri)
{
    var method: UUHttpMethod = UUHttpMethod.GET
    var headers: UUHttpHeaders = UUHttpHeaders()
    var body: UUHttpBody? = null
    var timeout: Int = DEFAULT_TIMEOUT
    var useGZipCompression: Boolean = true
    var proxy: Proxy? = null
    //var responseParser: UUByteArrayParser<SuccessType>? = null
    //var errorParser: UUErrorParser<ErrorType>? = null
    var socketFactory: SSLSocketFactory? = null
    var hostNameVerifier: HostnameVerifier? = null
    var authorizationProvider: UUHttpAuthorizationProvider? = null

    var responseHandler: UUHttpResponseHandler = UUBaseResponseHandler()

    companion object
    {
        var DEFAULT_TIMEOUT = (60 * UUDate.Constants.millisInOneSecond).toInt()
    }

    var startTime: Long = 0

    fun start()
    {
        startTime = System.currentTimeMillis()
    }

    internal open fun openConnection(): UUResult<HttpURLConnection>
    {
        var result: UUResult<HttpURLConnection>? = null

        try
        {
            val url = uri.fullUrl

            var urlConnection = if (proxy != null)
            {
                url.openConnection(proxy) as? HttpURLConnection
            }
            else
            {
                url.openConnection() as? HttpURLConnection
            }

            if (urlConnection == null)
            {
                return UUResult.failure(UUHttpError.create(UUHttpErrorCode.OpenConnectionFailure))
            }

            urlConnection.connectTimeout = timeout
            urlConnection.readTimeout = timeout
            urlConnection.doInput = true
            urlConnection.requestMethod = method.toString()

            if (useGZipCompression)
            {
                urlConnection.uuPutHeader(UUHttpHeader.AcceptEncoding, "gzip")
            }

            if (urlConnection is HttpsURLConnection)
            {
                socketFactory?.let()
                {
                    urlConnection.sslSocketFactory = it
                }

                hostNameVerifier?.let()
                {
                    urlConnection.hostnameVerifier = it
                }
            }

            start()
            result = UUResult.success(urlConnection)
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "openConnection", "", ex)
            result = UUResult.failure(UUHttpError.fromException(UUHttpErrorCode.OpenConnectionFailure, ex))
        }

        return result
    }

    open fun serializeBody(): UUResult<ByteArray?>
    {
        try
        {
            val actualBody = body ?: run()
            {
                return UUResult.success(null)
            }

            val encodedBody = actualBody.encodeBody() ?: run()
            {
                return UUResult.failure(UUHttpError.create(UUHttpErrorCode.SERIALIZE_FAILURE))
            }

            val encodedBodyLength = encodedBody.size
            if (encodedBodyLength > 0)
            {
                actualBody.uuSetHeaders(headers, encodedBodyLength)
                return UUResult.success(encodedBody)
            }
            else
            {
                // No exceptions thrown but a non-null UUHttpBody object should result in a
                // non null payload
                return UUResult.failure(UUHttpError.create(UUHttpErrorCode.SERIALIZE_FAILURE))
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "serializeBody", "", ex)
            return UUResult.failure(UUHttpError.fromException(UUHttpErrorCode.SERIALIZE_FAILURE, ex))
        }
    }

    open fun applyHeaders(connection: HttpURLConnection)
    {
        headers.log("applyHeaders", "RequestHeaders")

        headers.forEach()
        { key, value ->
            connection.setRequestProperty(key, value.joinToString(","))
        }
    }
}

open class UUTypedHttpRequest<SuccessType: Any, ErrorType: Any>(uri: UUHttpUri,
                                                                successClass: Class<SuccessType>,
                                                                errorClass: Class<ErrorType>,
    ):  UUHttpRequest(uri)
{
//    var successParser: UUByteArrayParser<SuccessType> =
//        { (data, contentType, contentEncoding) ->
//            return UUJson.fromBytes(data, successClass)
//        }
//
//    var errorParser: UUErrorParser<ErrorType>? = null

    init
    {
        responseHandler = UUTypedResponseHandler<SuccessType, ErrorType>(successClass, errorClass)


        /*
        if (request.responseParser == null)
        {
            request.responseParser =
                { data, contentType, contentEncoding ->
                    parseSuccess(responseClass, data, contentType, contentEncoding)
                }
        }

        if (request.errorParser == null)
        {
            request.errorParser = this::parseError
        }

        protected open fun parseError(data: ByteArray, contentType: String, contentEncoding: String, httpCode: Int): ErrorType?
        {
            return UUJson.fromBytes(data, errorClass)
        }*/
    }
    /*var method: UUHttpMethod = UUHttpMethod.GET
    var headers: UUHttpHeaders = UUHttpHeaders()
    var body: UUHttpBody? = null
    var timeout: Int = DEFAULT_TIMEOUT
    var useGZipCompression: Boolean = true
    var proxy: Proxy? = null
    var responseParser: UUByteArrayParser<SuccessType>? = null
    var errorParser: UUErrorParser<ErrorType>? = null
    var socketFactory: SSLSocketFactory? = null
    var hostNameVerifier: HostnameVerifier? = null
    var authorizationProvider: UUHttpAuthorizationProvider? = null

    companion object
    {
        var DEFAULT_TIMEOUT = (60 * UUDate.Constants.millisInOneSecond).toInt()
    }

    var startTime: Long = 0*/
}



