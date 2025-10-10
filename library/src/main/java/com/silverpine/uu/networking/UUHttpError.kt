package com.silverpine.uu.networking

import android.os.Bundle
import android.os.Parcelable
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUJson
import kotlinx.serialization.Serializable
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLException

object UUHttpError
{
    const val DOMAIN = "UUHttpErrorDomain"
    //const val HTTP_CODE_KEY = "httpCode"
    //const val USER_INFO_KEY = "userInfo"

    enum class UserInfoKeys(val key: String)
    {
        ErrorMessage("UUHttpSessionHttpErrorMessage"),
        AppResponse("UUHttpSessionAppResponse"),
        HttpMethod("UUHttpSessionErrorHttpMethod"),
        RequestUrl("UUHttpSessionErrorRequestUrl"),
        HttpCode("UUHttpSessionErrorHttpStatusCode"),
    }

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
        info.putParcelable(UserInfoKeys.AppResponse.key, userInfo)
        info.putInt(UserInfoKeys.HttpCode.key, httpCode)

        return UUError(adjustedCode.value, DOMAIN, userInfo = info)
    }

    fun create(request: UUHttpRequest, statusCode: Int, jsonResponse: String?): UUError
    {
        var adjustedCode = UUHttpErrorCode.HTTP_FAILURE

        if (statusCode == 401)
        {
            adjustedCode = UUHttpErrorCode.AUTHORIZATION_NEEDED
        }

        val info = Bundle()
        info.fillFromRequest(request)
        info.putString(UserInfoKeys.AppResponse.key, jsonResponse)
        info.putInt(UserInfoKeys.HttpCode.key, statusCode)

        return UUError(adjustedCode.value, DOMAIN, userInfo = info)

    }

    private fun Bundle.fillFromRequest(request: UUHttpRequest)
    {
        putString(UserInfoKeys.HttpMethod.key, request.method.name)
        putString(UserInfoKeys.RequestUrl.key, request.uri.fullUrl.toString())
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
        return userInfo?.getInt(UUHttpError.UserInfoKeys.HttpCode.key)
    }

    return null
}

fun Int.uuIsHttpSuccess(): Boolean
{
    return this in 200..299
}







/*


public let UUHttpSessionErrorDomain         = "UUHttpSessionErrorDomain"
public let UUHttpSessionHttpErrorMessageKey = "UUHttpSessionHttpErrorMessage"
public let UUHttpSessionAppResponseKey      = "UUHttpSessionAppResponse"
public let UUHttpSessionErrorHttpMethodKey      = "UUHttpSessionErrorHttpMethod"
public let UUHttpSessionErrorRequestUrlKey      = "UUHttpSessionErrorRequestUrl"
public let UUHttpSessionErrorHttpStatusCodeKey  = "UUHttpSessionErrorHttpStatusCode"

class UUErrorFactory
{
    static func createInvalidRequestError(_ request : UUHttpRequest) -> Error
    {
        var md: [String : Any]  = [:]

        fillFromRequest(&md, request.httpRequest)

        return createError(.invalidRequest, md)
    }

    static func wrapNetworkError(_ underlyingError: Error, _ request: UUHttpRequest) -> Error
    {
        var errCode: UUHttpSessionError = .httpFailure

        var md: [String : Any]  = [:]

        fillFromRequest(&md, request.httpRequest)
        fillFromUnderlyingError(&md, underlyingError)

        let nsError = underlyingError as NSError
                if (nsError.domain == NSURLErrorDomain)
                {
                    switch (nsError.code)
                    {
                        case NSURLErrorCannotFindHost:
                        errCode = .cannotFindHost

                                case NSURLErrorNotConnectedToInternet:
                        errCode = .noInternet

                                case NSURLErrorTimedOut:
                        errCode = .timedOut

                                case NSURLErrorCancelled:
                        errCode = .userCancelled

                                default:
                        errCode = .httpFailure
                    }
                }

        return createError(errCode, md)
    }

    static func createParseError(_ underlyingError: Error, _ data: Data, _ response: HTTPURLResponse, _ request: URLRequest) -> Error
    {
        var md: [String : Any]  = [:]

        fillFromRequest(&md, request)
        fillFromResponse(&md, response)
        fillFromUnderlyingError(&md, underlyingError)

        return createError(.parseFailure, md)
    }

    static func createHttpError(_ request: UUHttpRequest, _ httpResponseCode: Int, _ parsedResponse: Any?) -> Error
    {
        var md: [String : Any]  = [:]
        fillFromRequest(&md, request.httpRequest)
        md[UUHttpSessionAppResponseKey] = parsedResponse
        md[UUHttpSessionErrorHttpStatusCodeKey] = NSNumber(value: httpResponseCode)
        md[UUHttpSessionHttpErrorMessageKey] = HTTPURLResponse.localizedString(forStatusCode: httpResponseCode)
        md[NSLocalizedDescriptionKey] = HTTPURLResponse.localizedString(forStatusCode: httpResponseCode)

        var errorCode = UUHttpSessionError.httpError
        if (httpResponseCode == 401)
        {
            errorCode = .authorizationNeeded
        }

        return createError(errorCode, md)
    }

    static func createError(_ code: UUHttpSessionError, _ userInfo: [String:Any]?) -> Error
    {
        return NSError(domain: UUHttpSessionErrorDomain, code: code.rawValue, userInfo: userInfo)
    }

    private static func fillFromRequest(_ md: inout [String:Any], _ request: URLRequest?)
    {
        if let req = request
                {
                    md[UUHttpSessionErrorHttpMethodKey] = req.httpMethod
                    md[UUHttpSessionErrorRequestUrlKey] = req.url?.absoluteString
                }
    }

    private static func fillFromResponse(_ md: inout [String:Any], _ response: HTTPURLResponse?)
    {
        if let resp = response
                {
                    md[UUHttpSessionErrorHttpStatusCodeKey] = resp.statusCode
                }
    }

    private static func fillFromUnderlyingError(_ md: inout [String:Any], _ error: Error?)
    {
        if let e = error
                {
                    md[NSUnderlyingErrorKey] = e
                    md[NSLocalizedDescriptionKey] = e.localizedDescription

                    let nsError = e as NSError
                            md[NSLocalizedRecoverySuggestionErrorKey] = nsError.localizedRecoverySuggestion
                }
    }
}


public extension NSError
{
    var uuHttpErrorCode: UUHttpSessionError?
    {
        if (domain == UUHttpSessionErrorDomain)
        {
            return UUHttpSessionError(rawValue: code)
        }

        return nil
    }

    var uuHttpStatusCode: Int?
    {
        return userInfo.uuGetInt(UUHttpSessionErrorHttpStatusCodeKey)
    }
}

public extension Error
{
    var uuHttpErrorCode: UUHttpSessionError?
    {
        return (self as NSError).uuHttpErrorCode
    }

    var uuHttpStatusCode: Int?
    {
        return (self as NSError).uuHttpStatusCode
    }
}
*/