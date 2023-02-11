package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuUtf8ByteArray

abstract class UUHttpBody(val contentType: String, val content: Any)
{
    abstract fun serializeBody(): ByteArray?
}

open class UUJsonBody<T : Any>(content: T): UUHttpBody("application/json", content)
{
    override fun serializeBody(): ByteArray?
    {
        val json = UUJson.toJson(content, content.javaClass)
        return json?.uuUtf8ByteArray()
    }
}
