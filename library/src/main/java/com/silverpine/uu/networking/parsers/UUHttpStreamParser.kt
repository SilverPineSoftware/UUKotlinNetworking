package com.silverpine.uu.networking.parsers

import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Parses an HTTP response body from stream.
 *
 * Implementations are invoked from [com.silverpine.uu.networking.handlers.UUBaseResponseHandler]
 * on a background dispatcher. Prefer [uuHttpStreamParser] for inline lambdas instead of
 * `UUHttpStreamParser { }` SAM syntax, which does not correctly adapt suspending lambdas at runtime.
 */
fun interface UUHttpStreamParser
{
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
 */
fun uuHttpStreamParser(
    block: suspend (stream: InputStream, response: HttpURLConnection) -> Any?,
): UUHttpStreamParser =
    UUHttpStreamParser { stream, response -> block(stream, response) }