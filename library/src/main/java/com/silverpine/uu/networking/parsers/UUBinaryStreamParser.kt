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
 * I/O errors during [com.silverpine.uu.core.uuReadAll] are logged and returned as failures containing the original exception.
 *
 * @see UUHttpStreamParser
 * @see com.silverpine.uu.networking.handlers.UUBaseResponseHandler
 */
open class UUBinaryStreamParser : UUHttpStreamParser
{
    /**
     * @return success containing the full body as a [ByteArray] (empty for an empty stream), or a read failure.
     */
    override suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection,
    ): Result<Any?>
    {
        return stream.uuReadAll()
    }
}
