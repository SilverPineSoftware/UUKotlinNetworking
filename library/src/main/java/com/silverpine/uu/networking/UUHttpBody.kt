package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuUtf8ByteArray

open class UUHttpBody(var contentType: String)
{
    private var content: ByteArray? = null

    constructor(contentType: String, content: ByteArray?): this(contentType)
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