package com.silverpine.uu.networking

enum class UUHttpErrorCode(val value: Int)
{
    // Returned when URLSession returns a non-nil error and the underlying
    // error domain is NSURLErrorDomain and the underlying error code is
    // NSURLErrorNotConnectedToInternet
    noInternet(0x1000),

    // Returned when URLSession returns a non-nil error and the underlying
    // error domain is NSURLErrorDomain and the underlying error code is
    // NSURLErrorCannotFindHost
    cannotFindHost(0x1001),

    // Returned when URLSession returns a non-nil error and the underlying
    // error domain is NSURLErrorDomain and the underlying error code is
    // NSURLErrorTimedOut
    timedOut(0x1002),

    // Returned when URLSession completion block returns a non-nil Error, and
    // that error is not specifically mapped to a more common UUHttpSessionError
    // In this case, the underlying NSError is wrapped in the user info block
    // using the NSUnderlyingError key
    httpFailure(0x2000),

    // Returned when the URLSession completion block returns with a nil Error
    // and an HTTP return code that is not 2xx
    httpError(0x2001),

    // Returned when a user cancels an operation
    userCancelled(0x2002),

    // The request URL and/or query string parameters resulted in an invalid
    // URL.
    invalidRequest(0x2003),

    // UU failed to parse a response.  See underlying error for more details
    parseFailure(0x2004),

    // Error code returned when server authorization is needed.  By default this happens on a 401 error, but
    //applications can emit this error from custom parsers to trigger authorization renewal for other error conditions.
    authorizationNeeded(0x2005),

    // Error code returned when an unkown error occurs.  This is typically a developer error. Contact the Api developers.
    unkownError(0xFFFF);

    companion object
    {
        fun fromInt(value: Int): UUHttpErrorCode
        {
            values().forEach()
            {
                if (it.value == value)
                {
                    return it
                }
            }

            return unkownError
        }
    }
}