package com.silverpine.uu.networking

interface UUTypedHttpSession<ErrorType>
{
    fun <ResponseType> executeRequest(request: UUTypedHttpRequest<ResponseType, ErrorType>, completion: (UUTypedHttpResponse<ResponseType, ErrorType>) -> Unit)
    fun cancelAll()
}