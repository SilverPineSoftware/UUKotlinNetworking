package com.silverpine.uu.networking.authorization

import com.silverpine.uu.core.uuAsciiByteArray
import com.silverpine.uu.core.uuBase64

open class UUBasicAuthorizationProvider(
    var userName: String?,
    var password: String?
): UUHttpAuthorizationProvider("Basic", null)
{
    override fun formatAuthorization(): String?
    {
        val user = userName ?: return null
        val pwd = password ?: return null

        if (user.isNotEmpty() && pwd.isNotEmpty())
        {
            val authorizationData = "$user:$pwd".uuAsciiByteArray() ?: return null
            return authorizationData.uuBase64().getOrNull()
        }

        return null
    }
}