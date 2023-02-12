package com.silverpine.uu.networking

import com.silverpine.uu.core.UUDate
import java.net.Proxy

typealias UUByteArrayParser<T> = ((ByteArray,String,String)->T?)
typealias UUErrorParser<T> = ((ByteArray,String,String,Int)->T?)

open class UUTypedHttpRequest<SuccessType, ErrorType>(
    val uri: UUHttpUri,
    val method: UUHttpMethod = UUHttpMethod.GET,
    val headers: UUHttpHeaders = UUHttpHeaders(),
    val body: UUHttpBody? = null,
    val timeout: Int = DEFAULT_TIMEOUT,
    val useGZipCompression: Boolean = true,
    val proxy: Proxy? = null,
    var responseParser: UUByteArrayParser<SuccessType>? = null,
    var errorParser: UUErrorParser<ErrorType>? = null)
{
    companion object
    {
        var DEFAULT_TIMEOUT = (60 * UUDate.MILLIS_IN_ONE_SECOND).toInt()
    }

    var startTime: Long = 0
}



