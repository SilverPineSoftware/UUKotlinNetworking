package com.silverpine.uu.networking

import android.os.Bundle
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

    /**
     * An exception was caught when attempting to open the URL Connection. Check the exception
     * property of UUError for additional details.
     */
    OpenConnectionFailure(1006),

    // UU failed to parse a response.  See underlying error for more details
    PARSE_FAILURE(1007),

    /**
     * An exception was caught when serializing the request body. Check the exception
     * property of UUError for additional details.
     */
    SERIALIZE_FAILURE(1008),

    /**
     * An exception was caught when sending data to a remote endpoint. Check the exception
     * property of UUError for additional details.
     */
    WRITE_FAILED(1009),

    /**
     * An exception was caught when reading data from a remote endpoint. Check the exception
     * property of UUError for additional details.
     */
    READ_FAILED(1010),

    // Error code returned when server authorization is needed.  By default this happens on a 401 error, but
    //applications can emit this error from custom parsers to trigger authorization renewal for other error conditions.
    AUTHORIZATION_NEEDED(1011),

    // Error code returned when an unknown error occurs.  This is typically a developer error. Contact the Api developers.
    UNHANDLED_EXCEPTION(1012),

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
            val info = Bundle()
            info.putParcelable("userInfo", userInfo)

            return UUError(code.value, DOMAIN, ex, info)
        }
    }
}