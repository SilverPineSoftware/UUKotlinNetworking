package com.silverpine.uu.networking

import android.os.Bundle
import com.silverpine.uu.core.UUError
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLException

object UUNetworkError
{
    const val DOMAIN = "UUHttpErrorDomain"

    // const val USER_INFO_KEY_ERROR_MESSAGE = "UUHttpSessionHttpErrorMessage"
    const val USER_INFO_KEY_APP_RESPONSE = "UUHttpSessionAppResponse"
    const val USER_INFO_KEY_HTTP_METHOD = "UUHttpSessionErrorHttpMethod"
    const val USER_INFO_KEY_REQUEST_URL = "UUHttpSessionErrorRequestUrl"
    const val USER_INFO_KEY_HTTP_STATUS_CODE = "UUHttpSessionErrorHttpStatusCode"

    fun fromException(code: UUNetworkErrorCode, exception: Exception, request: UUHttpRequest?): UUError
    {
        var adjustedCode = code

        if (exception is SocketTimeoutException)
        {
            adjustedCode = UUNetworkErrorCode.TIMEOUT
        }
        else if (exception is UnknownHostException)
        {
            adjustedCode = UUNetworkErrorCode.CANNOT_FIND_HOST
        }
        else if (exception is SocketException ||
                exception is CertPathValidatorException ||
                exception is SSLException)
        {
            adjustedCode = UUNetworkErrorCode.HTTP_ERROR
        }

        return makeError(adjustedCode, request, exception)
    }

    /*
    fun fromHttpCode(httpCode: Int, userInfo: Parcelable?): UUError
    {
        var adjustedCode = UUNetworkErrorCode.HTTP_FAILURE

        if (httpCode == 401)
        {
            adjustedCode = UUNetworkErrorCode.AUTHORIZATION_NEEDED
        }

        val info = Bundle()
        info.putParcelable(USER_INFO_KEY_APP_RESPONSE, userInfo)
        info.putInt(USER_INFO_KEY_HTTP_STATUS_CODE, httpCode)

        return makeError(adjustedCode, userInfo = info)
    }*/

    fun create(request: UUHttpRequest, statusCode: Int, jsonResponse: String?): UUError
    {
        var adjustedCode = UUNetworkErrorCode.HTTP_FAILURE

        if (statusCode == 401)
        {
            adjustedCode = UUNetworkErrorCode.AUTHORIZATION_NEEDED
        }

        val info = Bundle()
        info.fillFromRequest(request)
        info.putString(USER_INFO_KEY_APP_RESPONSE, jsonResponse)
        info.putInt(USER_INFO_KEY_HTTP_STATUS_CODE, statusCode)

        return makeError(adjustedCode, userInfo = info)
    }

    fun makeError(
        code: UUNetworkErrorCode,
        request: UUHttpRequest? = null,
        exception: Exception? = null,
        userInfo: Bundle? = null,
    ): UUError
    {
        val info = userInfo ?: Bundle()

        request?.let()
        {
            info.fillFromRequest(it)
        }

        val err = UUError(code.value, DOMAIN, exception, info)
        err.errorDescription = code.errorDescription
        err.errorResolution = code.errorResolution
        return err
    }

    private fun Bundle.fillFromRequest(request: UUHttpRequest)
    {
        putString(USER_INFO_KEY_HTTP_METHOD, request.method.name)
        putString(USER_INFO_KEY_REQUEST_URL, request.toURL.toString())
    }
}

fun UUError.uuNetworkErrorCode(): UUNetworkErrorCode?
{
    if (domain == UUNetworkError.DOMAIN)
    {
        return UUNetworkErrorCode.fromInt(code)
    }

    return null
}

fun UUError.uuNetworkStatusCode(): Int?
{
    if (domain == UUNetworkError.DOMAIN)
    {
        return userInfo?.getInt(UUNetworkError.USER_INFO_KEY_HTTP_STATUS_CODE)
    }

    return null
}

fun UUError.uuNetworkHttpMethod(): String?
{
    if (domain == UUNetworkError.DOMAIN)
    {
        return userInfo?.getString(UUNetworkError.USER_INFO_KEY_HTTP_STATUS_CODE)
    }

    return null
}

fun UUError.uuNetworkRequestUrl(): String?
{
    if (domain == UUNetworkError.DOMAIN)
    {
        return userInfo?.getString(UUNetworkError.USER_INFO_KEY_REQUEST_URL)
    }

    return null
}

fun UUError.uuNetworkAppResponse(): String?
{
    if (domain == UUNetworkError.DOMAIN)
    {
        return userInfo?.getString(UUNetworkError.USER_INFO_KEY_APP_RESPONSE)
    }

    return null
}

fun Int.uuIsHttpSuccess(): Boolean
{
    return this in 200..299
}
