package com.silverpine.uu.networking

interface UUTokenAuthorizationProvider: UUHttpAuthorizationProvider
{
    override fun attachAuthorization(headers: UUHttpHeaders)
    {
        val t = token ?: return
        if (t.isNotEmpty())
        {
            headers.putSingle("Authorization", "Bearer $t")
        }
    }

    val token: String?
}