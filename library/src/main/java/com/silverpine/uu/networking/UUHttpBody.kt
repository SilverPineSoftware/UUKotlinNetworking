package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuUtf8ByteArray

open class UUHttpBody(var contentType: String, var contentEncoding: String? = null)
{
    private var content: ByteArray? = null

    constructor(contentType: String, content: ByteArray?, contentEncoding: String? = null): this(contentType, contentEncoding)
    {
        this.content = content
    }

    open fun encodeBody(): ByteArray?
    {
        return content
    }
}

class UUJsonBody<T: Any>(private val jsonObject: T): UUHttpBody(UUContentType.APPLICATION_JSON)
{
    override fun encodeBody(): ByteArray?
    {
        val json = UUJson.toJson(jsonObject, jsonObject.javaClass)
        return json?.uuUtf8ByteArray()
    }
}

fun UUHttpBody.uuSetHeaders(headers: UUHttpHeaders, requestBodyLength: Int)
{
    headers.putSingle("Content-Type", contentType)
    headers.putSingle("Content-Length", "$requestBodyLength")

    contentEncoding?.let()
    { contentEncoding ->
        headers.putSingle("Content-Encoding", contentEncoding)
    }
}