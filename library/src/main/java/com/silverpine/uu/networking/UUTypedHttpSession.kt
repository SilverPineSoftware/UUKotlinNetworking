package com.silverpine.uu.networking

interface UUTypedHttpSession
{
    fun <ResponseType, ErrorType> executeRequest(request: UUTypedHttpRequest<ResponseType, ErrorType>, completion: (UUTypedHttpResponse<ResponseType, ErrorType>) -> Unit)
    fun cancelAll()
}