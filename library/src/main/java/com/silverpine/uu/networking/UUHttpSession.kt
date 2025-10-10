package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUObjectBlock
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.OutputStream
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

    var logResponses: Boolean = false

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
            UULog.d(javaClass, "executeRequest", "Timeout: ${request.timeout}")

            val serializedBody = request.serializeBody().getOrElse()
            { error ->
                return UUHttpResponse(
                    request = request,
                    error = error)
            }

            request.applyHeaders(urlConnection)

            serializedBody?.let()
            {
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(it.size)
                val writeError = writeRequest(urlConnection, it)
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

            return handleResponse(request, urlConnection)
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

    private fun writeRequest(connection: HttpURLConnection, body: ByteArray): UUError?
    {
        var os: OutputStream? = null

        try
        {
            UULog.d(javaClass, "executeRequest", "RequestBody: ${body.uuUtf8().getOrNull()}")
            os = BufferedOutputStream(connection.outputStream)
            os.write(body)
            os.flush()
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "writeRequest", "", ex)
            return UUHttpError.fromException(UUHttpErrorCode.WRITE_FAILED, ex)
        }
        finally
        {
            os?.uuSafeClose()
        }

        return null
    }

    private fun HttpURLConnection?.uuSafeDisconnect()
    {
        try
        {
            this?.disconnect()
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "uuSafeDisconnect", "", ex)
        }
    }

    private suspend fun handleResponse(request: UUHttpRequest, urlConnection: HttpURLConnection): UUHttpResponse
    {
        try
        {
            return request.responseHandler.handleResponse(request, urlConnection)
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "handleResponse", "", ex)

            return UUHttpResponse(
                request = request,
                error = UUHttpError.fromException(UUHttpErrorCode.HandleResponseException, ex)
            )
        }
    }
}