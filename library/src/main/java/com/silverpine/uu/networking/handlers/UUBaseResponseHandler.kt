package com.silverpine.uu.networking.handlers

import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUHttpLogging
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.UUNetworkError
import com.silverpine.uu.networking.UUNetworkErrorCode
import com.silverpine.uu.networking.parsers.UUBinaryStreamParser
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import com.silverpine.uu.networking.uuIsHttpSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

open class UUBaseResponseHandler : UUHttpResponseHandler
{
    override suspend fun handleResponse(request: UUHttpRequest, urlConnection: HttpURLConnection): UUHttpResponse
    {
        try
        {
            val isSuccess = urlConnection.responseCode.uuIsHttpSuccess()

            var readStream = if (isSuccess)
            {
                urlConnection.inputStream
            }
            else
            {
                urlConnection.errorStream
            }

            when (urlConnection.contentEncoding?.lowercase())
            {
                "gzip" -> readStream =
                    withContext(Dispatchers.IO)
                    {
                        GZIPInputStream(readStream)
                    }

                "deflate" -> readStream = InflaterInputStream(readStream)
            }

            val bufferedStream = readStream.buffered()

            UUHttpLogging.logResponse(request, bufferedStream)

            val parser = if (isSuccess) successParser else errorParser
            val parsedResponse = withContext(Dispatchers.IO)
            {
                parser.parse(bufferedStream, urlConnection)
            }
            return finishHandleResponse(request, urlConnection, parsedResponse)
        }
        catch (ex: Exception)
        {
            return UUHttpResponse(
                request = request,
                error = UUNetworkError.fromException(UUNetworkErrorCode.READ_FAILED, ex, request)
            )
        }
    }

    private fun finishHandleResponse(
        request: UUHttpRequest,
        response: HttpURLConnection,
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
            err = UUNetworkError.create(request, httpStatusCode, jsonResponse)
        }

        val uuResponse = UUHttpResponse(
            request = request,
            response = response,
            error = err,
            parsedResponse = parsedResponse
        )

        return uuResponse
    }

    override val successParser: UUHttpStreamParser = UUBinaryStreamParser()

    override val errorParser: UUHttpStreamParser = UUBinaryStreamParser()
}