package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import java.net.HttpURLConnection

/**
 * Processes a completed [HttpURLConnection] into a [UUHttpResponse].
 *
 * Implementations are invoked from [com.silverpine.uu.networking.UUHttpSession.handleResponse] after
 * status codes and headers are available. The default implementation is [UUBaseResponseHandler], which
 * selects [successParser] or [errorParser] based on the HTTP status and parses the body stream.
 *
 * Provide a custom handler on [UUHttpRequest.responseHandler] for typed JSON, file downloads, or other
 * response shapes. Pre-built variants include [UUTypedResponseHandler] and [UUFileResponseHandler].
 *
 * @see UUBaseResponseHandler
 * @see UUTypedResponseHandler
 * @see UUFileResponseHandler
 * @see UUHttpStreamParser
 */
interface UUHttpResponseHandler
{
    /**
     * Reads [urlConnection], parses the body, and builds a [UUHttpResponse].
     *
     * @param request the originating HTTP request.
     * @param urlConnection open connection with response code and body stream available.
     * @return parsed response; may include [UUHttpResponse.error] for HTTP or parse failures.
     */
    suspend fun handleResponse(
        request: UUHttpRequest,
        urlConnection: HttpURLConnection,
    ): UUHttpResponse

    /**
     * Parser used when [HttpURLConnection.responseCode] indicates success (2xx).
     */
    val successParser: UUHttpStreamParser

    /**
     * Parser used when [HttpURLConnection.responseCode] indicates a non-success status.
     */
    val errorParser: UUHttpStreamParser
}
