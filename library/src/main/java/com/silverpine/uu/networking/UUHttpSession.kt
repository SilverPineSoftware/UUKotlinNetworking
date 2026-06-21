package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import java.net.CookieHandler
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

open class UUHttpSession
{
    open fun cancelAll()
    {
    }

    suspend fun execute(request: UUHttpRequest): UUHttpResponse
    {
        var urlConnection: HttpURLConnection? = null

        try
        {
            changeState(request, UUHttpRequest.State.CheckConnection)
            checkConnection(request)?.let()
            { error ->
                UUHttpLogging.logError(request, error)
                return UUHttpResponse(request = request, error = error)
            }

            changeState(request, UUHttpRequest.State.OpenConnection)
            urlConnection = openConnection(request).getOrElse()
            { error ->
                UUHttpLogging.logError(request, error)
                return UUHttpResponse(request = request, error = error)
            }

            UUHttpLogging.log(UUHttpLoggingMode.Request, request, "${request.method} ${urlConnection.url}")
            UUHttpLogging.log(UUHttpLoggingMode.Request, request, "Connect Timeout: ${request.connectTimeout}")
            UUHttpLogging.log(UUHttpLoggingMode.Request, request, "Read Timeout: ${request.readTimeout}")

            changeState(request, UUHttpRequest.State.PrepareToSend)
            val preparedBody = request.body?.prepareToSend()?.getOrElse()
            { error ->
                UUHttpLogging.logError(request, error)
                return UUHttpResponse(request = request, error = error)
            }

            preparedBody?.second?.entries?.forEach()
            { entry ->
                request.headers[entry.key] = entry.value
            }

            UUHttpLogging.logHeaders(request, UUHttpLoggingMode.RequestHeaders, request.headers)

            urlConnection.uuSetHeaders(request.headers)

            preparedBody?.first?.let()
            {
                changeState(request, UUHttpRequest.State.WriteRequest)
                UUHttpLogging.logRequest(request, it)
                urlConnection.uuWriteBody(it).getOrThrow()
            }

            changeState(request, UUHttpRequest.State.PrepareToReceive)
            val httpCode = urlConnection.responseCode
            val contentType = urlConnection.contentType ?: ""
            val contentEncoding = urlConnection.contentEncoding ?: ""

            UUHttpLogging.log(UUHttpLoggingMode.Response, request, "Status Code: $httpCode")
            UUHttpLogging.log(UUHttpLoggingMode.Response, request, "Content-Type: $contentType")
            UUHttpLogging.log(UUHttpLoggingMode.Response, request, "Content-Encoding: $contentEncoding")

            val responseHeaders = UUHttpHeaders(urlConnection.headerFields)
            UUHttpLogging.logHeaders(request, UUHttpLoggingMode.ResponseHeaders, responseHeaders)

            changeState(request, UUHttpRequest.State.HandleResponse)
            val response = handleResponse(request, urlConnection)
            response.error?.let()
            {
                UUHttpLogging.logError(request, it)
            }

            changeState(request, UUHttpRequest.State.Complete)
            request.end()
            return response
        }
        catch (ex: Exception)
        {
            val error = UUNetworkError.fromException(UUNetworkErrorCode.UNDEFINED, ex, request)
            UUHttpLogging.logError(request, error)

            return UUHttpResponse(request = request, error = error)
        }
        finally
        {
            urlConnection?.uuSafeDisconnect()
        }
    }

    open suspend fun checkConnection(request: UUHttpRequest): UUError?
    {
        return request.connectivityProvider?.checkConnection()
    }

    open suspend fun openConnection(request: UUHttpRequest): UUResult<HttpURLConnection, UUError>
    {
        var result: UUResult<HttpURLConnection, UUError>?

        try
        {
            val url = request.toURL

            val urlConnection = url.uuOpenConnection(request.proxy) ?: return UUResult.failure(
                UUNetworkError.makeError(UUNetworkErrorCode.OPEN_CONNECTION_FAILURE, request = request)
            )

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
            val error = UUNetworkError.fromException(UUNetworkErrorCode.OPEN_CONNECTION_FAILURE, ex, request)
            result = UUResult.failure(error)
        }

        return result
    }

    open suspend fun handleResponse(request: UUHttpRequest, urlConnection: HttpURLConnection): UUHttpResponse
    {
        return try
        {
            request.responseHandler.handleResponse(request, urlConnection)
        }
        catch (ex: Exception)
        {
            UUHttpResponse(request = request, error = UUNetworkError.fromException(UUNetworkErrorCode.HANDLE_RESPONSE_EXCEPTION, ex, request))
        }
    }

    open suspend fun changeState(request: UUHttpRequest, state: UUHttpRequest.State)
    {
        request.state = state
    }
}