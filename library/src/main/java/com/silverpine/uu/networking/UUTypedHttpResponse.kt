package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

open class UUTypedHttpResponse<ResponseType, ErrorType>(val request: UUTypedHttpRequest<ResponseType, ErrorType>)
{
    var error: UUError? = null
    var rawResponse: ByteArray? = null
    var parsedResponse: ResponseType? = null
    var parsedError: ErrorType? = null

    var httpCode: Int = 0
    var contentType: String = ""
    var contentEncoding: String = ""
    var exception: Exception? = null
    val headers: UUHttpHeaders = UUHttpHeaders()

    constructor(other: UUTypedHttpResponse<ResponseType, ErrorType>): this(other.request)
    {
        error = other.error
        rawResponse = other.rawResponse
        parsedResponse = other.parsedResponse
        httpCode = other.httpCode
        contentType = other.contentType
        contentEncoding = other.contentEncoding
        exception = other.exception
        headers.putAll(other.headers)
    }

    val wasSuccessful: Boolean
        get()
        {
            return httpCode.uuIsHttpSuccess()
        }
}