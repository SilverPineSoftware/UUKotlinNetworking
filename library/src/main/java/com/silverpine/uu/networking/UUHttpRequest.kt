package com.silverpine.uu.networking

import com.silverpine.uu.core.UUDate
import java.net.Proxy

open class UUHttpRequest(
    val uri: UUHttpUri,
    val method: UUHttpMethod = UUHttpMethod.GET,
    val headers: UUHttpHeaders = UUHttpHeaders(),
    val body: UUHttpBody? = null,
    val timeout: Int = DEFAULT_TIMEOUT,
    val useGZipCompression: Boolean = true,
    val proxy: Proxy? = null,
    var authorizationProvider: UUHttpAuthorizationProvider? = null,
    var responseParser: UUHttpDataParser? = null,
    var errorParser: UUHttpDataParser? = null)
{
    companion object
    {
        var DEFAULT_TIMEOUT = (60 * UUDate.MILLIS_IN_ONE_SECOND).toInt()

        //public static var defaultCachePolicy : URLRequest.CachePolicy = .useProtocolCachePolicy
    }

    //public var cachePolicy : URLRequest.CachePolicy = UUHttpRequest.defaultCachePolicy

    var startTime: Long = 0

//    fun buildUrl(): URL?
//    {
//        val url: URL?
//
//        try
//        {
//            url = URL(formatUrl())
//            authorizationProvider?.attachAuthorization(this)
//        }
//        catch (ex: Exception)
//        {
//            return null
//        }
//
//        return url
//    }

    fun serializeBody(): ByteArray?
    {
        body?.let()
        { httpBody ->

            httpBody.serializeBody()?.let()
            { requestBody ->

                headers.putSingle("Content-Type", httpBody.contentType)
                headers.putSingle("Content-Length", "${requestBody.size}")

                if (useGZipCompression)
                {
                    // GZip it
                }

                //UUHttpHeaders(headerFields).log("executeRequest", "RequestHeader")
                //UULog.d(javaClass, "executeRequest", "RequestBody:\n\n${requestBody.uuUtf8()}\n\n")

                //uuWrite(requestBody)

                return requestBody
            }
        }

        return null
    }

//    private fun formatUrl(): String
//    {
//        val sb = StringBuilder()
//        sb.append(url)
//        sb.append(path.toString())
//        sb.append(query.toString())
//        return sb.toString()
//    }
}
