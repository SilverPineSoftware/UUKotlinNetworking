package com.silverpine.uu.networking

interface UUHttpSession
{
    fun executeRequest(request: UUHttpRequest, completion: (UUHttpResponse)->Unit)
    fun cancelAll()
}