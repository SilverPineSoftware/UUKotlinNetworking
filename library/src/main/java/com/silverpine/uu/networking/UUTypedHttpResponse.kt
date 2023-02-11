package com.silverpine.uu.networking

open class UUTypedHttpResponse<SuccessType, ErrorType>(val request: UUTypedHttpRequest<SuccessType, ErrorType>)
{
    var success: SuccessType? = null
    var error: ErrorType? = null

    var httpCode: Int = 0
    var contentType: String = ""
    var contentEncoding: String = ""
    //var exception: Exception? = null
    val headers: UUHttpHeaders = UUHttpHeaders()

//    constructor(other: UUTypedHttpResponse<ResponseType, ErrorType>): this(other.request)
//    {
//        error = other.error
//        rawResponse = other.rawResponse
//        parsedResponse = other.parsedResponse
//        httpCode = other.httpCode
//        contentType = other.contentType
//        contentEncoding = other.contentEncoding
//        exception = other.exception
//        headers.putAll(other.headers)
//    }

//    val wasSuccessful: Boolean
//        get()
//        {
//            return httpCode.uuIsHttpSuccess()
//        }
}