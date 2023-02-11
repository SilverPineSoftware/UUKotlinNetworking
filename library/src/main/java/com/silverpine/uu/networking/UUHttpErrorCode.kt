package com.silverpine.uu.networking

import android.os.Parcelable
import com.silverpine.uu.core.UUError

enum class UUHttpErrorCode(val value: Int)
{
    // Returned when URLSession returns a non-nil error and the underlying
    // error domain is NSURLErrorDomain and the underlying error code is
    // NSURLErrorNotConnectedToInternet
    NO_INTERNET(1000),

    // Returned when URLSession returns a non-nil error and the underlying
    // error domain is NSURLErrorDomain and the underlying error code is
    // NSURLErrorCannotFindHost
    CANNOT_FIND_HOST(1001),

    // Returned when URLSession returns a non-nil error and the underlying
    // error domain is NSURLErrorDomain and the underlying error code is
    // NSURLErrorTimedOut
    TIMED_OUT(1002),

    // Returned when URLSession completion block returns a non-nil Error, and
    // that error is not specifically mapped to a more common UUHttpSessionError
    // In this case, the underlying NSError is wrapped in the user info block
    // using the NSUnderlyingError key
    HTTP_FAILURE(1003),

    // Returned when the URLSession completion block returns with a nil Error
    // and an HTTP return code that is not 2xx
    HTTP_ERROR(1004),

    // Returned when a user cancels an operation
    USER_CANCELLED(1005),

    // The request URL and/or query string parameters resulted in an invalid
    // URL.
    INVALID_REQUEST(1006),

    // UU failed to parse a response.  See underlying error for more details
    PARSE_FAILURE(1007),

    // Error code returned when server authorization is needed.  By default this happens on a 401 error, but
    //applications can emit this error from custom parsers to trigger authorization renewal for other error conditions.
    AUTHORIZATION_NEEDED(1008),

    // Error code returned when an unknown error occurs.  This is typically a developer error. Contact the Api developers.
    UNHANDLED_EXCEPTION(1009),

    // Error code returned when an unknown error occurs.  This is typically a developer error. Contact the Api developers.
    UNDEFINED(1999);

    companion object
    {
        const val DOMAIN = "UUHttpErrorCodeDomain"

        fun fromInt(value: Int): UUHttpErrorCode
        {
            values().forEach()
            {
                if (it.value == value)
                {
                    return it
                }
            }

            return UNDEFINED
        }

        fun error(
            code: UUHttpErrorCode,
            ex: Exception? = null,
            userInfo: Parcelable? = null): UUError
        {
            return UUError(code.value, DOMAIN, ex, userInfo)
        }
    }
}