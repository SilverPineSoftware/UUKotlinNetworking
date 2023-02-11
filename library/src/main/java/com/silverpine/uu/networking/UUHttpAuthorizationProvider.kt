package com.silverpine.uu.networking

interface UUHttpAuthorizationProvider
{
    fun attachAuthorization(request: UUHttpRequest)
}