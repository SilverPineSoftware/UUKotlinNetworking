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
 * Deserialization and stream read failures are returned in [Result].
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
     * @return success containing the deserialized value, or failure if JSON parsing or reading fails.
     */
    override suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection,
    ): Result<Any?>
    {
        return UUJson.fromStream(stream, objectClass)
    }
}
