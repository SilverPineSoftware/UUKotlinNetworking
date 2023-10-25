package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

open class UURawHttpResponse(val request: UURawHttpRequest)
{
    var success: Any? = null
    var error: UUError? = null

    var httpCode: Int = 0
    var contentType: String = ""
    var contentEncoding: String = ""
    val headers: UUHttpHeaders = UUHttpHeaders()
}