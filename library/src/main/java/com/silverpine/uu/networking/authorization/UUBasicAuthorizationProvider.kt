package com.silverpine.uu.networking.authorization

import com.silverpine.uu.core.uuAsciiByteArray
import com.silverpine.uu.core.uuBase64
import com.silverpine.uu.networking.UUHttpHeaders

interface UUBasicAuthorizationProvider: UUHttpAuthorizationProvider
{
    val userName: String?
    val password: String?

    override fun attachAuthorization(headers: UUHttpHeaders)
    {
        val user = userName ?: return
        val pwd = password ?: return

        if (user.isNotEmpty() && pwd.isNotEmpty())
        {
            val authorizationData = "$user:$pwd".uuAsciiByteArray() ?: return
            val authStringBase64 = authorizationData.uuBase64()
            headers.putSingle("Authorization", "Basic $authStringBase64")
        }
    }
}