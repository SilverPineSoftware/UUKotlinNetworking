package com.silverpine.uu.networking

import com.silverpine.uu.core.UUResult
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.networking.authorization.UUHttpAuthorizationProvider
import java.net.CookieHandler
import java.net.HttpURLConnection
import java.net.Proxy
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

open class UUHttpRequest(
    var uri: UUHttpUri,
    var method: UUHttpMethod = UUHttpMethod.GET,
    var headers: UUHttpHeaders = UUHttpHeaders(),
    var body: UUHttpBody? = null,
    val useCaches: Boolean = false,
    val defaultUseCaches: Boolean = false,
    val instanceFollowRedirects: Boolean = true,
    val cookieHandler: CookieHandler? = null,
    val connectTimeout: Int = 60_000,
    val readTimeout: Int = 60_000,
    var proxy: Proxy? = null,
    var socketFactory: SSLSocketFactory? = null,
    var hostNameVerifier: HostnameVerifier? = null,
    var authorizationProvider: UUHttpAuthorizationProvider? = null,
    var responseHandler: UUHttpResponseHandler = UUBaseResponseHandler())
{
    var startTime: Long = 0
    var endTime: Long = 0

    fun start()
    {
        startTime = System.currentTimeMillis()
    }

    fun end()
    {
        endTime = System.currentTimeMillis()
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

            urlConnection.useCaches = useCaches
            urlConnection.defaultUseCaches = defaultUseCaches
            urlConnection.instanceFollowRedirects = instanceFollowRedirects

            CookieHandler.setDefault(cookieHandler)

            urlConnection.connectTimeout = connectTimeout
            urlConnection.readTimeout = readTimeout

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

            authorizationProvider?.attachAuthorization(this)

            urlConnection.doInput = true
            urlConnection.requestMethod = method.toString()

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
    init
    {
        responseHandler = UUTypedResponseHandler<SuccessType, ErrorType>(successClass, errorClass)
    }
}



