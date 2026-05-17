package com.silverpine.uu.networking

import android.os.Bundle
import android.os.Parcelable
import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUHttpErrorCode.Companion.DOMAIN
import com.silverpine.uu.networking.UUHttpErrorCode.Companion.error
import com.silverpine.uu.networking.UUHttpErrorCode.Companion.fromInt

/**
 * Typed error codes for [UUHttpSession] and related networking APIs.
 *
 * Values are stored on [UUError.code] under [UUHttpError.DOMAIN] (or [DOMAIN] for legacy lookups).
 * Resolve a stored code with [UUError.uuErrorCode].
 *
 * Transport failures are often normalized in [UUHttpError.fromException] (for example
 * [java.net.SocketTimeoutException] → [TIMEOUT], [java.net.UnknownHostException] → [CANNOT_FIND_HOST]).
 *
 * @property value Integer code persisted on [UUError].
 *
 * @see UUHttpError
 * @see UUHttpSession
 */
enum class UUHttpErrorCode(val value: Int)
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
     * HTTP status and parsed body may be present in [UUError] user info; see [UUHttpError.UserInfoKeys].
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
     * Emitted for HTTP 401 by default, or by custom response parsers via [UUHttpError.fromHttpCode].
     */
    AUTHORIZATION_NEEDED(12),

    /** An unexpected exception was not mapped to a more specific code. */
    UNHANDLED_EXCEPTION(13),

    /** [UUHttpSession] failed while handling the response in [UUHttpSession.handleResponse]. */
    HANDLE_RESPONSE_EXCEPTION(14),

    /** Device is on a captive portal or similar network that requires login before internet access. */
    CAPTIVE_NETWORK_LOGIN_NEEDED(15),

    /** Unknown or unrecognized code; also used as the result of [fromInt] when no constant matches. */
    UNDEFINED(-1),
    ;

    companion object
    {
        /**
         * Error domain string used by [error] when building a [UUError].
         *
         * Prefer [UUHttpError.DOMAIN] for new code paths that integrate with [UUHttpError] helpers.
         */
        const val DOMAIN = "UUHttpErrorCodeDomain"

        /**
         * Maps an integer [value] to the matching constant, or [UNDEFINED] if none match.
         */
        fun fromInt(value: Int): UUHttpErrorCode
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

        /**
         * Builds a [UUError] with [code], [DOMAIN], optional [ex], and optional [userInfo] in a [Bundle].
         */
        fun error(
            code: UUHttpErrorCode,
            ex: Exception? = null,
            userInfo: Parcelable? = null,
        ): UUError
        {
            val info = Bundle()
            info.putParcelable("userInfo", userInfo)

            return UUError(code.value, DOMAIN, ex, info)
        }
    }
}
