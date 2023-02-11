package com.silverpine.uu.networking

interface UUTokenAuthorizationProvider: UUHttpAuthorizationProvider
{
    override fun attachAuthorization(request: UUHttpRequest)
    {
        authorizationToken?.let()
        { authToken ->
            request.headers.putSingle("Authorization", "Bearer $authToken")
        }
    }

    var authorizationToken: String?
}