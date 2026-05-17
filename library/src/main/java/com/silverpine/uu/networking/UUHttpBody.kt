package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.core.uuUtf8ByteArray

open class UUHttpBody(var contentType: String, var contentEncoding: String? = null)
{
    private var content: ByteArray? = null

    constructor(contentType: String, content: ByteArray?, contentEncoding: String? = null): this(contentType, contentEncoding)
    {
        this.content = content
    }

    open fun encode(): ByteArray?
    {
        return content
    }

    open fun prepareToSend(): UUResult<Pair<ByteArray, UUHttpHeaders>?, UUError>
    {
        try
        {
            val encodedBody = encode() ?: run()
            {
                return UUResult.failure(UUNetworkError.makeError(UUNetworkErrorCode.SERIALIZE_FAILURE))
            }

            val encodedBodyLength = encodedBody.size
            if (encodedBodyLength > 0)
            {
                val headers = buildHeaders(encodedBodyLength)
                return UUResult.success(Pair(encodedBody, headers))
            }
            else
            {
                // No exceptions thrown but a non-null UUHttpBody object should result in a
                // non-null payload
                return UUResult.failure(UUNetworkError.makeError(UUNetworkErrorCode.SERIALIZE_FAILURE))
            }
        }
        catch (ex: Exception)
        {
            return UUResult.failure(UUNetworkError.fromException(UUNetworkErrorCode.SERIALIZE_FAILURE, ex, null))
        }
    }

    open fun buildHeaders(contentLength: Int): UUHttpHeaders
    {
        val headers = UUHttpHeaders()
        headers.put(UUHttpHeader.ContentType, contentType)
        headers.put(UUHttpHeader.ContentLength, "$contentLength")

        contentEncoding?.let()
        { contentEncoding ->
            headers.put(UUHttpHeader.ContentEncoding, contentEncoding)
        }

        return headers
    }
}

class UUJsonBody<T: Any>(private val jsonObject: T): UUHttpBody(UUContentType.ApplicationJson.value)
{
    override fun encode(): ByteArray?
    {
        val json = UUJson.toJson(jsonObject, jsonObject.javaClass).getOrNull()
        return json?.uuUtf8ByteArray()
    }
}