package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUNetworkErrorCode.Companion.fromInt

/**
 * Typed error codes for [UUHttpSession] and related networking APIs.
 *
 * Values are stored on [UUError.code] under [UUNetworkError.DOMAIN].
 * Resolve a stored code with [UUError.code].
 *
 * Transport failures are often normalized in [UUNetworkError.fromException] (for example
 * [java.net.SocketTimeoutException] → [TIMEOUT], [java.net.UnknownHostException] → [CANNOT_FIND_HOST]).
 *
 * @property value Integer code persisted on [UUError].
 *
 * @see UUNetworkError
 * @see UUHttpSession
 */
enum class UUNetworkErrorCode(val value: Int)
{
    /** Request completed without a networking-layer failure. */
    SUCCESS(0),

    /** Connect or read timed out ([java.net.SocketTimeoutException] and similar). */
    TIMEOUT(1),

    /** The operation was canceled by the user or caller. */
    USER_CANCELLED(2),

    /** No validated internet connection (connectivity check before the request is sent). */
    NO_INTERNET(3),

    /** Host name could not be resolved ([java.net.UnknownHostException]). */
    CANNOT_FIND_HOST(4),

    /**
     * A low-level HTTP or I/O failure that is not mapped to a more specific code.
     *
     * The causing [Exception] is attached on [UUError] when available.
     */
    HTTP_FAILURE(5),

    /**
     * The server returned a non-success HTTP status, or a socket/SSL failure was classified as an HTTP-layer error.
     *
     * HTTP status and parsed body may be present in [UUError] user info; see UUNetworkError.USER_INFO_** keys.
     */
    HTTP_ERROR(6),

    /**
     * [java.net.HttpURLConnection] could not be opened for the request URL.
     *
     * See the [Exception] on [UUError] for details.
     */
    OPEN_CONNECTION_FAILURE(7),

    /** Response body could not be parsed; see nested error information on [UUError]. */
    PARSE_FAILURE(8),

    /**
     * Request body serialization failed before the request was sent.
     *
     * See the [Exception] on [UUError] for details.
     */
    SERIALIZE_FAILURE(9),

    /**
     * Writing the request body to the connection failed.
     *
     * See the [Exception] on [UUError] for details.
     */
    WRITE_FAILED(10),

    /**
     * Reading the response from the connection failed.
     *
     * See the [Exception] on [UUError] for details.
     */
    READ_FAILED(11),

    /**
     * Credentials are missing or rejected; renew authorization and retry.
     *
     * Emitted for HTTP 401 by default, or by custom response parsers via [UUNetworkError.fromHttpCode].
     */
    AUTHORIZATION_NEEDED(12),

    /** An unexpected exception was not mapped to a more specific code. */
    UNHANDLED_EXCEPTION(13),

    /** [UUHttpSession] failed while handling the response in [UUHttpSession.handleResponse]. */
    HANDLE_RESPONSE_EXCEPTION(14),

    /** Device is on a captive portal or similar network that requires login before internet access. */
    CAPTIVE_NETWORK_LOGIN_NEEDED(15),

    /** Unknown or unrecognized code; also used as the result of [fromInt] when no constant matches. */
    UNDEFINED(-1);


    /**
     * @return a developer-friendly description of this networking error.
     */
    val errorDescription: String
        get() = when (this)
        {
            SUCCESS -> "The request completed successfully."
            TIMEOUT -> "The network request timed out."
            USER_CANCELLED -> "The network request was canceled."
            NO_INTERNET -> "No internet connection is available."
            CANNOT_FIND_HOST -> "The server host name could not be resolved."
            HTTP_FAILURE -> "The HTTP request failed."
            HTTP_ERROR -> "The server returned an HTTP error response."
            OPEN_CONNECTION_FAILURE -> "A connection to the server could not be opened."
            PARSE_FAILURE -> "The HTTP response could not be parsed."
            SERIALIZE_FAILURE -> "The request body could not be serialized."
            WRITE_FAILED -> "Writing the request body to the server failed."
            READ_FAILED -> "Reading the response from the server failed."
            AUTHORIZATION_NEEDED -> "Authorization is required or was rejected."
            UNHANDLED_EXCEPTION -> "An unhandled exception occurred during the request."
            HANDLE_RESPONSE_EXCEPTION -> "Processing the HTTP response failed."
            CAPTIVE_NETWORK_LOGIN_NEEDED -> "The network requires sign-in before internet access is available."
            UNDEFINED -> "An unknown networking error occurred."
        }

    /**
     * @return suggested recovery steps for this networking error.
     */
    val errorResolution: String
        get() = when (this)
        {
            SUCCESS -> "No action required."
            TIMEOUT -> "Check connectivity, verify the server is reachable, and retry or increase the request timeout."
            USER_CANCELLED -> "Retry the request if the operation is still needed."
            NO_INTERNET -> "Connect to Wi-Fi or cellular data, then retry the request."
            CANNOT_FIND_HOST -> "Verify the URL and DNS settings, then retry the request."
            HTTP_FAILURE -> "Inspect the attached exception and request details, then retry."
            HTTP_ERROR -> "Review the HTTP status code and response body, then adjust the request or server as needed."
            OPEN_CONNECTION_FAILURE -> "Verify the URL, TLS configuration, and network access, then retry."
            PARSE_FAILURE -> "Confirm the response format matches the parser and API contract."
            SERIALIZE_FAILURE -> "Verify the request model and encoding, then retry."
            WRITE_FAILED -> "Check the request payload and connection stability, then retry."
            READ_FAILED -> "Check server availability and connection stability, then retry."
            AUTHORIZATION_NEEDED -> "Refresh credentials or sign in again, then retry the request."
            UNHANDLED_EXCEPTION -> "Inspect the attached exception, handle or report the failure, and retry if appropriate."
            HANDLE_RESPONSE_EXCEPTION -> "Inspect the attached exception and response handler logic, then retry."
            CAPTIVE_NETWORK_LOGIN_NEEDED -> "Complete network sign-in (captive portal), then retry the request."
            UNDEFINED -> "Inspect error details and the attached exception; retry or report if the problem persists."
        }

    companion object
    {
        /**
         * Maps an integer [value] to the matching constant, or [UNDEFINED] if none match.
         */
        fun fromInt(value: Int): UUNetworkErrorCode
        {
            entries.forEach()
            {
                if (it.value == value)
                {
                    return it
                }
            }

            return UNDEFINED
        }
    }
}


