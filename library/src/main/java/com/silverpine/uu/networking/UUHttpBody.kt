package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuToByteArray
import java.nio.charset.Charset

open class UUHttpBody(
    var contentType: String,
    var contentEncoding: String)
{
    var content: ByteArray? = null

    constructor(contentType: String, contentEncoding: String, content: ByteArray?): this(contentType, contentEncoding)
    {
        this.content = content
    }

    open fun encodeBody(): ByteArray?
    {
        return content
    }
}

class UUJsonBody<T: Any>(private val jsonObject: T, private val charset: Charset = Charsets.UTF_8): UUHttpBody(UUContentType.APPLICATION_JSON, charset.name())
{
    override fun encodeBody(): ByteArray?
    {
        val json = UUJson.toJson(jsonObject, jsonObject.javaClass)
        return json?.uuToByteArray(charset)
    }
}