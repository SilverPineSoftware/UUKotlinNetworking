package com.silverpine.uu.networking

interface UUTypedHttpAuthorizationProvider
{
    fun <ResponseType, ErrorType> attachAuthorization(request: UUTypedHttpRequest<ResponseType, ErrorType>)
}