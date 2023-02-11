package com.silverpine.uu.networking

interface UUTokenAuthorizationProvider: UUHttpAuthorizationProvider
{
    override fun attachAuthorization(headers: UUHttpHeaders)
    {
        token?.let { headers.putSingle("Authorization", "Bearer $it") }
    }

    var token: String?
}