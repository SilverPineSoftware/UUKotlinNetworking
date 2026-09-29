package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuReadAll
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * [UUHttpStreamParser] that reads the response body as text.
 *
 * The body is loaded with [com.silverpine.uu.core.uuReadAll] and decoded with [String] using the
 * platform default charset (UTF-8 on Android). Use [UUTypedStreamParser] when the body is JSON and
 * should be deserialized into a model.
 *
 * @see UUBinaryStreamParser
 * @see UUTypedStreamParser
 */
class UUTextResponseParser : UUHttpStreamParser
{
    /**
     * @return success containing the response body as a [String] (empty for an empty stream), or a read failure.
     */
    override suspend fun parse(stream: InputStream, response: HttpURLConnection): Result<Any?>
    {
        return stream.uuReadAll().map { String(it) }
    }
}
