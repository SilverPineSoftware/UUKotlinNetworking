package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import java.net.HttpURLConnection

open class UUHttpResponse(
    val request: UUHttpRequest,
    val response: HttpURLConnection? = null,
    val headers: UUHttpHeaders? = response?.headerFields?.let { UUHttpHeaders(it) },
    val error: UUError? = null,
    val parsedResponse: Any? = null,
    val endTime: Long = System.currentTimeMillis()
)
{
    val httpStatusCode: Int
        get()
        {
            return response?.responseCode ?: 0
        }
}

