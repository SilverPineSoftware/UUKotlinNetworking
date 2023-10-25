package com.silverpine.uu.networking

import android.os.Parcelable
import com.silverpine.uu.core.UUDate
import com.silverpine.uu.core.UUError
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Proxy


typealias UURawStreamParser = (InputStream)->Any?

open class UURawHttpRequest(var uri: UUHttpUri)
{
    var method: UUHttpMethod = UUHttpMethod.GET
    var headers: UUHttpHeaders = UUHttpHeaders()
    var body: UUHttpBody? = null
    var timeout: Int = DEFAULT_TIMEOUT
    var useGZipCompression: Boolean = true
    var proxy: Proxy? = null
    var successResponseHandler: UURawStreamParser = { null }
    var errorResponseHandler: UURawStreamParser = { null }

    companion object
    {
        var DEFAULT_TIMEOUT = (60 * UUDate.MILLIS_IN_ONE_SECOND).toInt()
    }

    var startTime: Long = 0

    protected fun openConnection(): HttpURLConnection?
    {
        var urlConnection: HttpURLConnection? = null

        try
        {
            val url = uri.fullUrl

            urlConnection = if (proxy != null)
            {
                url.openConnection(proxy) as? HttpURLConnection
            }
            else
            {
                url.openConnection() as? HttpURLConnection
            }

            urlConnection?.connectTimeout = timeout
            urlConnection?.readTimeout = timeout
            urlConnection?.doInput = true
            urlConnection?.requestMethod = method.toString()

            if (useGZipCompression)
            {
                urlConnection?.setRequestProperty("Accept-Encoding", "gzip")
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "openConnection", "", ex)
        }

        return urlConnection
    }

    protected fun serializeBody(): Pair<ByteArray?, UUError?>
    {
        var requestBody: ByteArray? = null
        var requestBodyLength: Int
        var error: UUError? = null
        try
        {
            body?.let()
            { body ->

                requestBody = body.encodeBody()
                requestBodyLength = requestBody?.size ?: 0

                if (requestBodyLength > 0)
                {
                    headers.putSingle("Content-Type", body.contentType)
                    headers.putSingle("Content-Length", "$requestBodyLength")
                }
                else
                {
                    // No exceptions thrown but a non-null UUHttpBody object should result in a
                    // non null payload
                    error = UUHttpError.create(UUHttpErrorCode.SERIALIZE_FAILURE)
                }
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "serializeBody", "", ex)
            error = UUHttpError.fromException(UUHttpErrorCode.SERIALIZE_FAILURE, ex)
        }

        return Pair(requestBody, error)
    }

    protected fun applyHeaders(urlConnection: HttpURLConnection)
    {
        headers.log("applyHeaders", "RequestHeaders")

        headers.forEach()
        { key, value ->
            urlConnection.setRequestProperty(key, value.joinToString(","))
        }
    }

    protected var job: Job? = null

    fun execute(completion: (UURawHttpResponse) -> Unit)
    {
        job = CoroutineScope(Dispatchers.IO).launch()
        {
            val response = internalExecute()
            completion(response)
        }
    }

    fun cancel()
    {
        job?.cancel()
    }

    protected fun internalExecute(): UURawHttpResponse
    {
        val response = UURawHttpResponse(this)

        var urlConnection: HttpURLConnection? = null

        try
        {
            urlConnection = openConnection()
            if (urlConnection == null)
            {
                response.error = UUHttpError.create(UUHttpErrorCode.OpenConnectionFailure)
                return response
            }

            startTime = System.currentTimeMillis()

            UULog.d(javaClass, "executeRequest", "$method ${urlConnection.url} ")
            UULog.d(javaClass, "executeRequest", "Timeout: $timeout")

            val (requestBody, serializeError) = serializeBody()
            if (serializeError != null)
            {
                response.error = serializeError
                return response
            }

            applyHeaders(urlConnection)

            requestBody?.let()
            {
                val writeError = writeRequest(urlConnection, it)
                if (writeError != null)
                {
                    response.error = writeError
                    return response
                }
            }

            response.httpCode = urlConnection.responseCode
            response.contentType = urlConnection.contentType ?: ""
            response.contentEncoding = urlConnection.contentEncoding ?: ""

            UULog.d(javaClass,"executeRequest", "HTTP Response Code: ${response.httpCode}")
            UULog.d(javaClass,"executeRequest", "Response Content-Type: ${response.contentType}")
            UULog.d(javaClass,"executeRequest", "Response Content-Encoding: ${response.contentEncoding}")

            response.headers.putAll(urlConnection.headerFields)

            response.headers.log("executeRequest", "ResponseHeader")

            downloadResponse(urlConnection, response)

            if (response.error != null)
            {
                return response
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "executeRequest", "", ex)
            response.error = UUHttpError.fromException(UUHttpErrorCode.UNDEFINED, ex)
        }
        finally
        {
            urlConnection?.uuSafeDisconnect()
        }

        return response
    }

    protected fun writeRequest(connection: HttpURLConnection, body: ByteArray): UUError?
    {
        try
        {
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use()
            { os ->
                os.write(body)
                os.flush()
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "writeRequest", "", ex)
            return UUHttpError.fromException(UUHttpErrorCode.WRITE_FAILED, ex)
        }

        return null
    }

    protected fun HttpURLConnection?.uuSafeDisconnect()
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

    protected fun downloadResponse(urlConnection: HttpURLConnection, response: UURawHttpResponse)
    {
        try
        {
            if (urlConnection.responseCode.uuIsHttpSuccess())
            {
                response.success = response.request.successResponseHandler(urlConnection.inputStream)
            }
            else
            {
                val errorResponse = response.request.errorResponseHandler(urlConnection.errorStream)
                if (errorResponse is UUError)
                {
                    response.error = errorResponse
                }
                else
                {
                    response.error = UUHttpError.fromHttpCode(response.httpCode, errorResponse as? Parcelable)
                }
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "downloadResponse", "", ex)
            response.error = UUHttpError.fromException(UUHttpErrorCode.READ_FAILED, ex)
        }
    }
}



