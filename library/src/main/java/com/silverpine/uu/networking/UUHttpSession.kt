package com.silverpine.uu.networking

import com.silverpine.uu.core.UUObjectBlock
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection

open class UUHttpSession
{
    open fun executeRequest(request: UUHttpRequest, completion: UUObjectBlock<UUHttpResponse>)
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val response = executeRequestSync(request)
            completion(response)
        }
    }

    open fun cancelAll()
    {
    }

    private suspend fun executeRequestSync(request: UUHttpRequest): UUHttpResponse
    {
        var urlConnection: HttpURLConnection? = null

        try
        {

            urlConnection = request.openConnection().getOrElse()
            { error ->
                return UUHttpResponse(
                    request = request,
                    error = error)
            }

            UULog.d(javaClass, "executeRequest", "${request.method} ${urlConnection.url} ")
            //UULog.d(javaClass, "executeRequest", "Timeout: ${request.timeout}")

            val preparedBody = request.body?.prepareToSend()?.getOrElse()
            { error ->
                return UUHttpResponse(
                    request = request,
                    error = error)
            }

            preparedBody?.second?.entries?.forEach()
            { entry ->
                request.headers.put(entry.key, entry.value)
            }

            urlConnection.uuSetHeaders(request.headers)

            preparedBody?.first?.let()
            {
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(it.size)
                val writeError = urlConnection.uuWriteBody(it)
                if (writeError != null)
                {
                    return UUHttpResponse(
                        request = request,
                        error = writeError)
                }
            }

            val httpCode = urlConnection.responseCode
            val contentType = urlConnection.contentType ?: ""
            val contentEncoding = urlConnection.contentEncoding ?: ""

            UULog.d(javaClass,"executeRequest", "HTTP Response Code: $httpCode")
            UULog.d(javaClass,"executeRequest", "Response Content-Type: $contentType")
            UULog.d(javaClass,"executeRequest", "Response Content-Encoding: $contentEncoding")

            val responseHeaders = UUHttpHeaders(urlConnection.headerFields)
            responseHeaders.log("executeRequest", "ResponseHeader")

            val response = urlConnection.uuHandleResponse(request)
            request.end()
            return response
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "executeRequest", "", ex)

            return UUHttpResponse(
                request = request,
                error = UUHttpError.fromException(UUHttpErrorCode.UNDEFINED, ex))
        }
        finally
        {
            urlConnection?.uuSafeDisconnect()
        }
    }
}