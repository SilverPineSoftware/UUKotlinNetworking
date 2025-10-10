package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import java.io.InputStream
import java.net.HttpURLConnection

interface UUHttpResponseHandler
{
    suspend fun handleResponse(
        request: UUHttpRequest,
        response: HttpURLConnection,
        stream: InputStream
    ): UUHttpResponse

    val successParser: UUHttpStreamParser
    val errorParser: UUHttpStreamParser
}

open class UUBaseResponseHandler() : UUHttpResponseHandler
{
    override suspend fun handleResponse(
        request: UUHttpRequest,
        connection: HttpURLConnection,
        stream: InputStream,
    ): UUHttpResponse
    {
        val parser = if (connection.responseCode.uuIsHttpSuccess())
        {
            request.responseHandler.successParser
        }
        else
        {
            request.responseHandler.errorParser
        }

        val parsedResponse = parser.parse(stream, connection)
        return finishHandleResponse(request, connection,/* responseBytes,*/ parsedResponse)
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

open class UUPassthroughResponseHandler: UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUBinaryStreamParser()
    override val errorParser: UUHttpStreamParser = UUBinaryStreamParser()
}