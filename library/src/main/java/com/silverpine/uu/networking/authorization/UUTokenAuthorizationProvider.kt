package com.silverpine.uu.networking.authorization

import com.silverpine.uu.networking.UUHttpHeaders

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