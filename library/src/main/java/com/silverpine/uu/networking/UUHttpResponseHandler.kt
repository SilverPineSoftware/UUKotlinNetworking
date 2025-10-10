package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.logging.UULog
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

interface UUHttpResponseHandler
{
    suspend fun handleResponse(
        request: UUHttpRequest,
        response: HttpURLConnection,
    ): UUHttpResponse

    val successParser: UUHttpStreamParser
    val errorParser: UUHttpStreamParser
}

open class UUBaseResponseHandler() : UUHttpResponseHandler
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
                "gzip" -> readStream = GZIPInputStream(readStream)
                "deflate" -> readStream = InflaterInputStream(readStream)
            }

            /*if (logResponses)
            {
                UULog.d(javaClass, "downloadResponse", "ResponseBody: ${readStream.uuReadAll()?.uuUtf8()?.getOrNull()}")
            }*/

            //return request.responseHandler.handleResponse(request, urlConnection, readStream)

            val parser = if (isSuccess)
            {
                successParser
            }
            else
            {
                errorParser
            }

            val parsedResponse = parser.parse(readStream, urlConnection)
            return finishHandleResponse(request, urlConnection, parsedResponse)
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
            err = UUHttpError.create(request, httpStatusCode, jsonResponse)
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

open class UUTypedResponseHandler<SuccessType: Any, ErrorType: Any>(
    successClass: Class<SuccessType>,
    errorClass: Class<ErrorType>
): UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUTypedStreamParser<SuccessType>(successClass)
    override val errorParser: UUHttpStreamParser = UUTypedStreamParser<ErrorType>(errorClass)
}
