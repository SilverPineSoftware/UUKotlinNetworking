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

/**
 * Default [UUHttpResponseHandler] that reads the connection body and parses it with [UUHttpStreamParser].
 *
 * Behavior:
 * - Selects [inputStream][HttpURLConnection.getInputStream] for 2xx responses and
 *   [errorStream][HttpURLConnection.getErrorStream] otherwise.
 * - Wraps the stream for `gzip` and `deflate` [content encodings][HttpURLConnection.getContentEncoding].
 * - Invokes [successParser] or [errorParser] on [Dispatchers.IO].
 * - Maps non-success HTTP status codes to [UUNetworkError] when the parser did not already return a [UUError].
 * - Catches unexpected exceptions and returns [UUNetworkErrorCode.READ_FAILED].
 *
 * Default parsers are [UUBinaryStreamParser] for both success and error bodies.
 *
 * @see UUTypedResponseHandler
 * @see UUFileResponseHandler
 */
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
                error = UUNetworkError.fromException(UUNetworkErrorCode.READ_FAILED, ex, request),
            )
        }
    }

    private fun finishHandleResponse(
        request: UUHttpRequest,
        response: HttpURLConnection,
        result: Any?,
    ): UUHttpResponse
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
            parsedResponse = parsedResponse,
        )

        return uuResponse
    }

    /** @see UUHttpResponseHandler.successParser */
    override val successParser: UUHttpStreamParser = UUBinaryStreamParser()

    /** @see UUHttpResponseHandler.errorParser */
    override val errorParser: UUHttpStreamParser = UUBinaryStreamParser()
}
