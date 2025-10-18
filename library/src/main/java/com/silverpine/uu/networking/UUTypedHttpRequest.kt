package com.silverpine.uu.networking

import com.silverpine.uu.networking.authorization.UUHttpAuthorizationProvider
import java.net.CookieHandler
import java.net.Proxy
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

open class UUTypedHttpRequest<SuccessType: Any, ErrorType: Any>(
    url: String,
    path: UUPathArgs = arrayListOf(),
    query: UUQueryStringsArgs = hashMapOf(),
    method: UUHttpMethod = UUHttpMethod.GET,
    headers: UUHttpHeaders = UUHttpHeaders(),
    body: UUHttpBody? = null,
    useCaches: Boolean = false,
    defaultUseCaches: Boolean = false,
    instanceFollowRedirects: Boolean = true,
    cookieHandler: CookieHandler? = null,
    connectTimeout: Int = 60_000,
    readTimeout: Int = 60_000,
    proxy: Proxy? = null,
    socketFactory: SSLSocketFactory? = null,
    hostNameVerifier: HostnameVerifier? = null,
    authorizationProvider: UUHttpAuthorizationProvider? = null,
    loggingMode: Array<UUHttpLoggingMode> = UUHttpLoggingMode.Info,
    successClass: Class<SuccessType>,
    errorClass: Class<ErrorType>,
):  UUHttpRequest(
    url = url,
    path = path,
    query = query,
    method = method,
    headers = headers,
    body = body,
    useCaches = useCaches,
    defaultUseCaches = defaultUseCaches,
    instanceFollowRedirects = instanceFollowRedirects,
    cookieHandler = cookieHandler,
    connectTimeout = connectTimeout,
    readTimeout = readTimeout,
    proxy = proxy,
    socketFactory = socketFactory,
    hostNameVerifier = hostNameVerifier,
    authorizationProvider = authorizationProvider,
    responseHandler = UUTypedResponseHandler<SuccessType, ErrorType>(successClass, errorClass),
    loggingMode = loggingMode
)
