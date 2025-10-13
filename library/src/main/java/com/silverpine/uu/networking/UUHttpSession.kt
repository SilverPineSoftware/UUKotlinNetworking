package com.silverpine.uu.networking

import com.silverpine.uu.core.UUObjectBlock
import com.silverpine.uu.core.UUResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.CookieHandler
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

open class UUHttpSession
{
    open fun executeRequest(request: UUHttpRequest, completion: UUObjectBlock<UUHttpResponse>)
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val response = doOneRequest(request)
            completion(response)
        }
    }

    open fun cancelAll()
    {
    }

    private suspend fun doOneRequest(request: UUHttpRequest): UUHttpResponse
    {
        var urlConnection: HttpURLConnection? = null

        try
        {
            request.state = UUHttpRequest.State.OpenConnection
            urlConnection = openConnection(request).getOrElse()
            { error ->
                UUHttpLogging.logError(request, error)
                return UUHttpResponse(request = request, error = error)
            }

            UUHttpLogging.log(UUHttpLoggingMode.Request, request, "${request.method} ${urlConnection.url}")
            UUHttpLogging.log(UUHttpLoggingMode.Request, request, "Connect Timeout: ${request.connectTimeout}")
            UUHttpLogging.log(UUHttpLoggingMode.Request, request, "Read Timeout: ${request.readTimeout}")

            request.state = UUHttpRequest.State.PrepareToSend
            val preparedBody = request.body?.prepareToSend()?.getOrElse()
            { error ->
                UUHttpLogging.logError(request, error)
                return UUHttpResponse(request = request, error = error)
            }

            preparedBody?.second?.entries?.forEach()
            { entry ->
                request.headers.put(entry.key, entry.value)
            }

            UUHttpLogging.logHeaders(request, UUHttpLoggingMode.RequestHeaders, request.headers)

            urlConnection.uuSetHeaders(request.headers)

            preparedBody?.first?.let()
            {
                request.state = UUHttpRequest.State.WriteRequest
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(it.size)
                val writeError = urlConnection.uuWriteBody(it)
                if (writeError != null)
                {
                    UUHttpLogging.logError(request, writeError)
                    return UUHttpResponse(request = request, error = writeError)
                }
            }

            request.state = UUHttpRequest.State.PrepareToReceive
            val httpCode = urlConnection.responseCode
            val contentType = urlConnection.contentType ?: ""
            val contentEncoding = urlConnection.contentEncoding ?: ""

            UUHttpLogging.log(UUHttpLoggingMode.Response, request, "Status Code: $httpCode")
            UUHttpLogging.log(UUHttpLoggingMode.Response, request, "Content-Type: $contentType")
            UUHttpLogging.log(UUHttpLoggingMode.Response, request, "Content-Encoding: $contentEncoding")

            val responseHeaders = UUHttpHeaders(urlConnection.headerFields)
            UUHttpLogging.logHeaders(request, UUHttpLoggingMode.ResponseHeaders, responseHeaders)

            request.state = UUHttpRequest.State.HandleResponse
            val response = handleResponse(request, urlConnection)
            response.error?.let()
            {
                UUHttpLogging.logError(request, it)
            }

            request.state = UUHttpRequest.State.Complete
            request.end()
            return response
        }
        catch (ex: Exception)
        {
            val error = UUHttpError.fromException(UUHttpErrorCode.UNDEFINED, ex)
            UUHttpLogging.logError(request, error)

            return UUHttpResponse(request = request, error = error)
        }
        finally
        {
            urlConnection?.uuSafeDisconnect()
        }
    }

    open fun openConnection(request: UUHttpRequest): UUResult<HttpURLConnection>
    {
        var result: UUResult<HttpURLConnection>? = null

        try
        {
            val url = request.uri.fullUrl

            var urlConnection = url.uuOpenConnection(request.proxy)

            if (urlConnection == null)
            {
                return UUResult.failure(UUHttpError.create(UUHttpErrorCode.OpenConnectionFailure))
            }

            urlConnection.useCaches = request.useCaches
            urlConnection.defaultUseCaches = request.defaultUseCaches
            urlConnection.instanceFollowRedirects = request.instanceFollowRedirects

            CookieHandler.setDefault(request.cookieHandler)

            urlConnection.connectTimeout = request.connectTimeout
            urlConnection.readTimeout = request.readTimeout

            if (urlConnection is HttpsURLConnection)
            {
                request.socketFactory?.let()
                {
                    urlConnection.sslSocketFactory = it
                }

                request.hostNameVerifier?.let()
                {
                    urlConnection.hostnameVerifier = it
                }
            }

            request.authorizationProvider?.attachAuthorization(request)

            urlConnection.doInput = true
            urlConnection.requestMethod = request.method.toString()

            request.start()
            result = UUResult.success(urlConnection)
        }
        catch (ex: Exception)
        {
            val error = UUHttpError.fromException(UUHttpErrorCode.OpenConnectionFailure, ex)
            result = UUResult.failure(error)
        }

        return result
    }

    open suspend fun handleResponse(request: UUHttpRequest, urlConnection: HttpURLConnection): UUHttpResponse
    {
        try
        {
            return request.responseHandler.handleResponse(request, urlConnection)
        }
        catch (ex: Exception)
        {
            return UUHttpResponse(request = request, error = UUHttpError.fromException(UUHttpErrorCode.HandleResponseException, ex))
        }
    }
}