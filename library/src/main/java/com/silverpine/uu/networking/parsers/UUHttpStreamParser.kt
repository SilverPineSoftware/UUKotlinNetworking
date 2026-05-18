package com.silverpine.uu.networking.parsers

import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Parses an HTTP response body from an [InputStream].
 *
 * Parsers are selected by [com.silverpine.uu.networking.handlers.UUHttpResponseHandler.successParser]
 * and [com.silverpine.uu.networking.handlers.UUHttpResponseHandler.errorParser], then invoked from
 * [com.silverpine.uu.networking.handlers.UUBaseResponseHandler] on [kotlinx.coroutines.Dispatchers.IO]
 * after the connection body stream is opened (and optional gzip/deflate decoding).
 *
 * The parsed value is stored on [com.silverpine.uu.networking.UUHttpResponse.parsedResponse]. Returning
 * a [com.silverpine.uu.core.UUError] from [parse] is supported by the response handler pipeline.
 *
 * ### Custom parsers
 * Prefer [uuHttpStreamParser] for inline lambdas. Do not use `UUHttpStreamParser { }` SAM syntax:
 * it compiles but does not correctly adapt suspending lambdas at runtime.
 *
 * @see uuHttpStreamParser
 * @see UUBinaryStreamParser
 * @see UUTextResponseParser
 * @see UUTypedStreamParser
 * @see UUDownloadFileStreamParser
 * @see com.silverpine.uu.networking.handlers.UUBaseResponseHandler
 */
fun interface UUHttpStreamParser
{
    /**
     * Reads and converts [stream] into a response object.
     *
     * @param stream body stream from the [HttpURLConnection]; may already be buffered or wrapped
     *   (for example gzip). Consumed by the parser implementation.
     * @param response the connection that produced [stream]; use [HttpURLConnection.url],
     *   [HttpURLConnection.contentType], or [HttpURLConnection.responseCode] when needed.
     * @return the parsed payload (for example [ByteArray], [String], a model instance, or [java.io.File]),
     *   or `null` when parsing fails or there is no content.
     */
    suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection,
    ): Any?
}

/**
 * Creates a [UUHttpStreamParser] from a suspending lambda.
 *
 * Use this instead of `UUHttpStreamParser { stream, response -> ... }`, which compiles but can fail
 * at runtime because SAM conversion does not wire up the suspending functional interface correctly.
 *
 * ### Example
 * ```kotlin
 * val parser = uuHttpStreamParser { stream, _ ->
 *     stream.bufferedReader().readText()
 * }
 * ```
 *
 * @param block suspending parser implementation.
 * @see UUHttpStreamParser
 */
fun uuHttpStreamParser(
    block: suspend (stream: InputStream, response: HttpURLConnection) -> Any?,
): UUHttpStreamParser =
    object : UUHttpStreamParser
    {
        override suspend fun parse(stream: InputStream, response: HttpURLConnection): Any? =
            block(stream, response)
    }
