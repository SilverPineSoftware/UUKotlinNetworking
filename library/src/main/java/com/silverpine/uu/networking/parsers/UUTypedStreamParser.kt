package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.UUJson
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * [UUHttpStreamParser] that deserializes a JSON response body into [DataType] using [UUJson].
 *
 * The target type must be supported by the active [com.silverpine.uu.core.UUJsonProvider]
 * (typically [kotlinx.serialization] via [com.silverpine.uu.core.UUKotlinXJsonProvider]).
 *
 * Used as the success and error parser by [com.silverpine.uu.networking.handlers.UUTypedResponseHandler].
 * Deserialization failures are swallowed; [parse] returns `null` rather than throwing.
 *
 * ### Example
 * ```kotlin
 * val parser = UUTypedStreamParser(MyDto::class.java)
 * request.responseHandler = UUTypedResponseHandler(MyDto::class.java, ApiError::class.java)
 * ```
 *
 * @param DataType model type to deserialize from JSON.
 * @property objectClass runtime class passed to [UUJson.fromStream].
 * @see UUJson
 * @see com.silverpine.uu.networking.handlers.UUTypedResponseHandler
 * @see UUTextResponseParser
 */
open class UUTypedStreamParser<DataType : Any>(private val objectClass: Class<DataType>) : UUHttpStreamParser
{
    /**
     * @return a deserialized instance of [DataType], or `null` if JSON parsing fails or the stream cannot be read.
     */
    override suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection,
    ): Any?
    {
        return UUJson.fromStream(stream, objectClass).getOrNull()
    }
}
