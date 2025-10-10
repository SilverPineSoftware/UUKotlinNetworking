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

            return downloadResponse(request, urlConnection)
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

    private suspend fun downloadResponse(request: UUHttpRequest, urlConnection: HttpURLConnection): UUHttpResponse
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

            /*val responseBytes = readStream.uuReadAll()
            if (responseBytes == null)
            {
                return UUHttpResponse(
                    request = request,
                    response = urlConnection
                )
            }*/

            if (logResponses)
            {
                //UULog.d(javaClass, "downloadResponse", "ResponseBody: ${responseBytes.uuUtf8().getOrNull()}")
            }

            return request.responseHandler.handleResponse(request, urlConnection, readStream)
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "downloadResponse", "", ex)

            return UUHttpResponse(
                request = request,
                error = UUHttpError.fromException(UUHttpErrorCode.READ_FAILED, ex)
            )
        }
    }

    /*
    private fun finishHandleResponse(
        request: UUHttpRequest,
        response: HttpURLConnection,
        data: ByteArray?,
        result: Any?): UUHttpResponse
    {
        var err: UUError? = null
        var parsedResponse: Any? = result

        (result as? UUError)?.let()
        {
            err = it
            parsedResponse = null
        }

        val httpStatusCode = response.responseCode

        // By default, the standard response parsers won't emit an Error, but custom response handlers might.
        // When callers parse response JSON and return Errors, we will honor that.
        if (err == null && !httpStatusCode.uuIsHttpSuccess())
        {
            val jsonResponse: String? = null  // from parsed response
            err = UUHttpError.create(request, httpStatusCode, jsonResponse)
        }

        val uuResponse = UUHttpResponse(
            request = request,
            response = response,
            error = err,
            rawResponse = data,
            parsedResponse = parsedResponse
        )

        return uuResponse
    }*/

    /*private fun parseResponse(byteArray: ByteArray, response: UUHttpResponse<SuccessType, ErrorType>)
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
    }*/
}


/*
open class UUTypedHttpSession<ErrorType>
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

            if (urlConnection is HttpsURLConnection)
            {
                urlConnection.sslSocketFactory = socketFactory
                urlConnection.hostnameVerifier = hostNameVerifier
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
                    body.uuSetHeaders(headers, requestBodyLength)
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

            val responseBytes = readStream.uuReadAll()
            if (responseBytes != null)
            {
                if (logResponses)
                {
                    UULog.d(javaClass, "executeRequest", "ResponseBody: ${responseBytes.uuUtf8()}")
                }

                parseResponse(responseBytes, response)
            }
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
}*/