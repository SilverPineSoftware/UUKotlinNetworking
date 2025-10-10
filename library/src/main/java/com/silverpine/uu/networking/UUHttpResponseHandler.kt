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

    var successParser: UUHttpStreamParser
    var errorParser: UUHttpStreamParser
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
        //data: ByteArray?,
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
            //rawResponse = data,
            parsedResponse = parsedResponse
        )

        return uuResponse
    }



    /*
    private fun downloadResponse(urlConnection: HttpURLConnection)
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

    private fun parseResponse(byteArray: ByteArray, response: UUHttpResponse<SuccessType, ErrorType>)
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
    */

/*
    open func handleResponse(request: UUHttpRequest, data: Data?, response: URLResponse?, error: Error?, completion: @escaping (UUHttpResponse)->())
    {
        if let e = error
                {
                    UULog.debug(tag: LOG_TAG, message: "Got an error: \(String(describing: error))")
                    let err = UUErrorFactory.wrapNetworkError(e, request)
                    finishHandleResponse(request: request, response: response, data: data, result: err, completion: completion)
                    return
                }

        guard let httpResponse = response as? HTTPURLResponse else
        {
            let err = UUErrorFactory.createError(UUHttpSessionError.unkownError, [:])
            finishHandleResponse(request: request, response: response, data: data, result: err, completion: completion)
            return
        }

        UULog.debug(tag: LOG_TAG, message: "HTTP Response Code: \(httpResponse.statusCode)")

        httpResponse.allHeaderFields.forEach()
        { (key: AnyHashable, value: Any) in
            UULog.debug(tag: LOG_TAG, message: "ResponseHeader: \(key) - \(value)")
        }

        // Verify there is response data to parse, if not, just finish the operation
        guard let data = data,
        !data.isEmpty,
        let httpResponse = response as? HTTPURLResponse,
        let urlRequest = request.httpRequest else
        {
            finishHandleResponse(request: request, response: response, data: data, result: nil, completion: completion)
            return
        }

        UULog.debug(tag: LOG_TAG, message: "ResponseBody: \(String(describing: String(bytes: data, encoding: .utf8)))")

        let parser = httpResponse.statusCode.uuIsHttpSuccess() ? successParser : errorParser

        parser.parse(data: data, response: httpResponse, request: urlRequest)
        { parseResult in

                self.finishHandleResponse(request: request, response: httpResponse, data: data, result: parseResult, completion: completion)
        }
    }

    private func finishHandleResponse(request: UUHttpRequest, response: URLResponse?, data: Data?, result: Any?, completion: @escaping (UUHttpResponse)->())
    {
        var err: Error? = nil
        var parsedResponse: Any? = result

        if let parseError = result as? Error
                {
                    err = parseError
                    parsedResponse = nil
                }

        let httpResponse = (response as? HTTPURLResponse)
        let httpStatusCode = httpResponse?.statusCode ?? 0

        // By default, the standard response parsers won't emit an Error, but custom response handlers might.
        // When callers parse response JSON and return Errors, we will honor that.
        if (err == nil && !isHttpSuccessResponseCode(httpStatusCode))
        {
            err = UUErrorFactory.createHttpError(request, httpStatusCode, parsedResponse)
        }

        let uuResponse = UUHttpResponse(request: request, response: httpResponse, error: err, rawResponse: data, parsedResponse: parsedResponse)
        completion(uuResponse)
    }

    private func isHttpSuccessResponseCode(_ responseCode : Int) -> Bool
    {
        return (responseCode >= 200 && responseCode < 300)
    }
    */
    override var successParser: UUHttpStreamParser = UUBinaryStreamParser()

    override var errorParser: UUHttpStreamParser = UUBinaryStreamParser()
}

/*
open class UUJsonCodableResponseHandler<SuccessType: Codable, ErrorType: Codable>: UUBaseResponseHandler
{
    public required init()
    {
        super.init()
    }

    public var jsonDecoder: JSONDecoder = JSONDecoder()

    open override var successParser: UUHttpDataParser
    {
        let parser = UUJsonCodableDataParser<SuccessType>()
        parser.jsonDecoder = self.jsonDecoder
        return parser
    }

    open override var errorParser: UUHttpDataParser
    {
        let parser = UUJsonCodableDataParser<ErrorType>()
        parser.jsonDecoder = self.jsonDecoder
        return parser
    }
}*/

open class UUTypedResponseHandler<SuccessType: Any, ErrorType: Any>(
    successClass: Class<SuccessType>,
    errorClass: Class<ErrorType>
): UUBaseResponseHandler()
{
    override var successParser: UUHttpStreamParser = UUTypedStreamParser<SuccessType>(successClass)
    override var errorParser: UUHttpStreamParser = UUTypedStreamParser<ErrorType>(errorClass)
}

open class UUPassthroughResponseHandler: UUBaseResponseHandler()
{
    override var successParser: UUHttpStreamParser = UUBinaryStreamParser()
    override var errorParser: UUHttpStreamParser = UUBinaryStreamParser()
}