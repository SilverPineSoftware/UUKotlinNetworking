package com.silverpine.uu.networking

import android.os.Bundle
import android.os.Parcelable
import com.silverpine.uu.core.UUError
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLException

object UUHttpError
{
    const val DOMAIN = "UUHttpErrorDomain"
    const val HTTP_CODE_KEY = "httpCode"
    const val USER_INFO_KEY = "userInfo"

    fun create(code: UUHttpErrorCode): UUError
    {
        return UUError(code.value, DOMAIN)
    }

    fun fromException(code: UUHttpErrorCode, exception: Exception): UUError
    {
        var adjustedCode = code

        if (exception is SocketTimeoutException)
        {
            adjustedCode = UUHttpErrorCode.TIMED_OUT
        }
        else if (exception is UnknownHostException)
        {
            adjustedCode = UUHttpErrorCode.CANNOT_FIND_HOST
        }
        else if (exception is SocketException ||
                exception is CertPathValidatorException ||
                exception is SSLException)
        {
            adjustedCode = UUHttpErrorCode.HTTP_ERROR
        }

        return UUError(adjustedCode.value, DOMAIN, exception)
    }

    fun fromHttpCode(httpCode: Int, userInfo: Parcelable?): UUError
    {
        var adjustedCode = UUHttpErrorCode.HTTP_FAILURE

        if (httpCode == 401)
        {
            adjustedCode = UUHttpErrorCode.AUTHORIZATION_NEEDED
        }

        val info = Bundle()
        info.putParcelable(USER_INFO_KEY, userInfo)
        info.putInt(HTTP_CODE_KEY, httpCode)

        return UUError(adjustedCode.value, DOMAIN, userInfo = info)
    }
}

fun UUError.uuErrorCode(): UUHttpErrorCode?
{
    if (domain == UUHttpError.DOMAIN)
    {
        return UUHttpErrorCode.fromInt(code)
    }

    return null
}

fun UUError.uuHttpStatusCode(): Int?
{
    if (domain == UUHttpError.DOMAIN)
    {
        return userInfo?.getInt(UUHttpError.HTTP_CODE_KEY)
    }

    return null
}

fun Int.uuIsHttpSuccess(): Boolean
{
    return this in 200..299
}