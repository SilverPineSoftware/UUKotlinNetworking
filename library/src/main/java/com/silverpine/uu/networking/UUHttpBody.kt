package com.silverpine.uu.networking

import com.silverpine.uu.core.uuToJson
import com.silverpine.uu.core.uuUtf8ByteArray

abstract class UUHttpBody(val contentType: String, val content: Any)
{
    abstract fun serializeBody(): ByteArray?
}

open class UUJsonBody<T : Any>(content: T): UUHttpBody(UUContentType.APPLICATION_JSON, content)
{
    override fun serializeBody(): ByteArray?
    {
        val json = content.uuToJson()
        return json?.uuUtf8ByteArray()
    }
}
