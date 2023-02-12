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

open class UUDefaultTypedHttpSession<ErrorType>: UUTypedHttpSession<ErrorType>
{
    override fun <ResponseType> executeRequest(request: UUTypedHttpRequest<ResponseType, ErrorType>, completion: (UUTypedHttpResponse<ResponseType, ErrorType>) -> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val response = executeRequestSync(request)
            completion.invoke(response)
        }
    }

    override fun cancelAll()
    {
    }

    private fun <ResponseType> executeRequestSync(request: UUTypedHttpRequest<ResponseType, ErrorType>): UUTypedHttpResponse<ResponseType, ErrorType>
    {
        val response = UUTypedHttpResponse(request)

        var urlConnection: HttpURLConnection? = null

        try
        {
            val openConnectionError = openConnection(request)
            if (openConnectionError.second != null)
            {
                response.error = openConnectionError.second
                return response
            }

            urlConnection = openConnectionError.first
            if (urlConnection == null)
            {
                response.error = UUHttpError.create(UUHttpErrorCode.INVALID_REQUEST)
                return response
            }

            request.startTime = System.currentTimeMillis()


//            if (urlConnection is HttpsURLConnection) {
//                val factory: SSLSocketFactory = request.getSocketFactory()
//                if (factory != null) {
//                    urlConnection.sslSocketFactory = factory
//                }
//            }

            UULog.d(javaClass, "executeRequest", "${request.method} ${urlConnection.url} ")
            UULog.d(javaClass, "executeRequest", "Timeout: ${request.timeout}")

            val requestBody = serializeBody(request, response)
            if (response.error != null)
            {
                return response
            }

            request.headers.log("executeRequest", "RequestHeaders")

            request.headers.forEach()
            { key, value ->
                urlConnection.setRequestProperty(key, value.joinToString(","))
            }

            if (requestBody != null)
            {
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(requestBody.size)
                val writeError = writeRequest(urlConnection, requestBody)
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

    private fun <SuccessType, ErrorType> openConnection(request: UUTypedHttpRequest<SuccessType, ErrorType>): Pair<HttpURLConnection?, UUError?>
    {
        val url = request.uri.toURL()
        if (url == null)
        {
            return Pair(null, UUHttpError.create(UUHttpErrorCode.INVALID_REQUEST))
        }

        val urlConnection = if (request.proxy != null)
        {
            url.openConnection(request.proxy) as? HttpURLConnection
        }
        else
        {
            url.openConnection() as? HttpURLConnection
        }

        urlConnection?.connectTimeout = request.timeout
        urlConnection?.readTimeout = request.timeout
        urlConnection?.doInput = true
        urlConnection?.requestMethod = request.method.toString()

        if (request.useGZipCompression)
        {
            urlConnection?.setRequestProperty("Accept-Encoding", "gzip")
        }

        return Pair(urlConnection, null)
    }

    private fun <SuccessType, ErrorType> serializeBody(request: UUTypedHttpRequest<SuccessType, ErrorType>, response: UUTypedHttpResponse<SuccessType, ErrorType>): ByteArray?
    {
        try
        {
            val httpBody = request.body ?: return null
            val requestBody = httpBody.serializeBody() ?: return null

            request.headers.putSingle("Content-Type", httpBody.contentType)
            request.headers.putSingle("Content-Length", "${requestBody.size}")

            return requestBody
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "serializeBody", "", ex)
            response.error = UUHttpError.fromException(UUHttpErrorCode.WRITE_FAILED, ex)
        }

        return null
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

    private fun <SuccessType, ErrorType> downloadResponse(urlConnection: HttpURLConnection, response: UUTypedHttpResponse<SuccessType, ErrorType>)
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

            parseResponse(bos.toByteArray(), response)
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "downloadResponse", "", ex)
            response.error = UUHttpError.fromException(UUHttpErrorCode.READ_FAILED, ex)
        }
    }

    private fun <SuccessType, ErrorType> parseResponse(byteArray: ByteArray, response: UUTypedHttpResponse<SuccessType, ErrorType>)
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