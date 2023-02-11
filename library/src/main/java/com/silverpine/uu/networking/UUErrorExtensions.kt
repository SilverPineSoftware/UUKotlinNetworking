package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

class UUErrorExtensions
{
}

fun UUError.uuHttpErrorCode(): UUHttpErrorCode?
{
    if (domain == "UUHttp.ERROR_DOMAIN")
    {
        return UUHttpErrorCode.fromInt(code)
    }

    return null
}

fun Int.uuIsHttpSuccess(): Boolean
{
    return this in 200..299
}