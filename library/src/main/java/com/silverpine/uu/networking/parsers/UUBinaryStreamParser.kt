package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuReadAll
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Default [UUHttpStreamParser] that reads the entire response body into a [ByteArray].
 *
 * This is the parser used by [com.silverpine.uu.networking.handlers.UUBaseResponseHandler] for both
 * success and error bodies when no custom handler overrides are supplied. Suitable for opaque
 * binary payloads, unknown content types, or when the caller will interpret bytes later.
 *
 * I/O errors during [com.silverpine.uu.core.uuReadAll] are logged and surfaced as a `null` return value.
 *
 * @see UUHttpStreamParser
 * @see com.silverpine.uu.networking.handlers.UUBaseResponseHandler
 */
open class UUBinaryStreamParser : UUHttpStreamParser
{
    /**
     * @return the full body as a [ByteArray], an empty array when the stream is empty, or `null` on read failure.
     */
    override suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection,
    ): Any?
    {
        return stream.uuReadAll()
    }
}
