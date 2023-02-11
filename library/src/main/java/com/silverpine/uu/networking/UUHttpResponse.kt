package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

open class UUHttpResponse(val request: UUHttpRequest)
{
    var error: UUError? = null
    var rawResponse: ByteArray? = null
    var parsedResponse: Any? = null

    var httpCode: Int = 0
    var contentType: String = ""
    var contentEncoding: String = ""
    var exception: Exception? = null
    val headers: UUHttpHeaders = UUHttpHeaders()

    val wasSuccessful: Boolean
        get()
        {
            return httpCode.uuIsHttpSuccess()
        }
}