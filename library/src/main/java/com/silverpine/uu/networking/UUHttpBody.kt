package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuToByteArray
import kotlinx.serialization.SerializationStrategy
import java.nio.charset.Charset


abstract class UUHttpBody<T: Any>(
    var contentType: String,
    var contentEncoding: String,
    var content: T?)
{
    var contentLength: Int = 0

    protected abstract fun serializeBody(content: T?): ByteArray?

    fun encodeBody(): ByteArray?
    {
        val body = serializeBody(content)
        contentLength = body?.size ?: 0
        return body
    }
}

class UUJsonBody<T: Any>(content: T?, private val serializationStrategy: SerializationStrategy<T>?): UUHttpBody<T>(UUContentType.APPLICATION_JSON, Charsets.UTF_8.toString(), content)
{
    private var json: String? = null

    constructor(json: String?): this(null, null)
    {
        this.json = json
    }
    override fun serializeBody(content: T?): ByteArray?
    {
        json?.let()
        {
            return it.uuToByteArray(Charset.forName(contentEncoding))
        }

        serializationStrategy?.let()
        {
            val json = UUJson.toJson(content, serializationStrategy)
            return json?.uuToByteArray(Charset.forName(contentEncoding))
        }

        return null
    }
}

/*
inline fun <reified T: Any> uuJsonBody(content: T): UUHttpBody
{
    return UUHttpBody(UUContentType.APPLICATION_JSON, content.uuToJson()?.uuUtf8ByteArray())
}
*/