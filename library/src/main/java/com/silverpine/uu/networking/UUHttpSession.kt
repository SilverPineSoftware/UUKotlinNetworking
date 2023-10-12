package com.silverpine.uu.networking

import android.os.Parcelable
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.OutputStream
import java.net.HttpURLConnection

open class UUHttpSession<ErrorType>
{
    open fun <ResponseType> executeRequest(request: UUHttpRequest<ResponseType, ErrorType>, completion: (UUHttpResponse<ResponseType, ErrorType>) -> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val response = executeRequestSync(request)
            completion.invoke(response)
        }
    }

    var logResponses: Boolean = false

    open fun cancelAll()
    {
    }

    private fun <ResponseType> executeRequestSync(request: UUHttpRequest<ResponseType, ErrorType>): UUHttpResponse<ResponseType, ErrorType>
    {
        val response = UUHttpResponse(request)

        var urlConnection: HttpURLConnection? = null

        try
        {
            urlConnection = request.openConnection()
            if (urlConnection == null)
            {
                response.error = UUHttpError.create(UUHttpErrorCode.OpenConnectionFailure)
                return response
            }

            request.startTime = System.currentTimeMillis()

            UULog.d(javaClass, "executeRequest", "${request.method} ${urlConnection.url} ")
            UULog.d(javaClass, "executeRequest", "Timeout: ${request.timeout}")

            val (requestBody, serializeError) = request.serializeBody()
            if (serializeError != null)
            {
                response.error = serializeError
                return response
            }

            request.applyHeaders(urlConnection)

            requestBody?.let()
            {
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(it.size)
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

    private fun UUHttpRequest<*,*>.openConnection(): HttpURLConnection?
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

    private fun UUHttpRequest<*,*>.serializeBody(): Pair<ByteArray?, UUError?>
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

    private fun UUHttpRequest<*,*>.applyHeaders(urlConnection: HttpURLConnection)
    {
        headers.log("applyHeaders", "RequestHeaders")

        headers.forEach()
        { key, value ->
            urlConnection.setRequestProperty(key, value.joinToString(","))
        }
    }

    private fun writeRequest(connection: HttpURLConnection, body: ByteArray): UUError?
    {
        var os: OutputStream? = null

        try
        {
            UULog.d(javaClass, "executeRequest", "RequestBody: ${body.uuUtf8()}")
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

    private fun Closeable?.uuSafeClose()
    {
        try
        {
            this?.close()
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "uuSafeClose", "", ex)
        }
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

    private fun <SuccessType, ErrorType> downloadResponse(urlConnection: HttpURLConnection, response: UUHttpResponse<SuccessType, ErrorType>)
    {
        try
        {
            val readStream = if (urlConnection.responseCode.uuIsHttpSuccess())
            {
                urlConnection.inputStream
            }
            else
            {
                urlConnection.errorStream
            }

            var bytesRead: Int
            val buffer = ByteArray(10240)
            val bos = ByteArrayOutputStream()

            while (true)
            {
                bytesRead = readStream.read(buffer, 0, buffer.size)
                if (bytesRead == -1)
                {
                    break
                }

                bos.write(buffer, 0, bytesRead)
            }

            val responseBytes = bos.toByteArray()

            if (logResponses)
            {
                UULog.d(javaClass, "executeRequest", "ResponseBody: ${responseBytes.uuUtf8()}")
            }
            
            parseResponse(responseBytes, response)
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "downloadResponse", "", ex)
            response.error = UUHttpError.fromException(UUHttpErrorCode.READ_FAILED, ex)
        }
    }

    private fun <SuccessType, ErrorType> parseResponse(byteArray: ByteArray, response: UUHttpResponse<SuccessType, ErrorType>)
    {
        try
        {
            if (response.httpCode.uuIsHttpSuccess())
            {
                response.success = response.request.responseParser?.invoke(byteArray, response.contentType, response.contentEncoding)
            }
            else
            {
                val errorResponse = response.request.errorParser?.invoke(byteArray, response.contentType, response.contentEncoding, response.httpCode)
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
            UULog.d(javaClass, "parseResponse", "", ex)
            response.error = UUHttpError.fromException(UUHttpErrorCode.PARSE_FAILURE, ex)
        }
    }
}