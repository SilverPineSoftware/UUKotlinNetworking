package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

open class UUTypedHttpResponse<SuccessType: Any, ErrorType: Any>(
    val request: UUTypedHttpRequest<SuccessType, ErrorType>)
{
    var success: SuccessType? = null
    var error: UUError? = null

    var httpCode: Int = 0
    var contentType: String = ""
    var contentEncoding: String = ""
    val headers: UUHttpHeaders = UUHttpHeaders()
}