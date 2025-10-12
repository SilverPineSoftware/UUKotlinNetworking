package com.silverpine.uu.networking.authorization

import com.silverpine.uu.networking.UUHttpHeader
import com.silverpine.uu.networking.UUHttpHeaders

open class UUHttpAuthorizationProvider(
    var scheme: String,
    var authorization: String?
)
{
    open fun formatAuthorization(): String?
    {
        return authorization
    }

    open fun attachAuthorization(headers: UUHttpHeaders)
    {
        val t = formatAuthorization() ?: return
        if (t.isNotEmpty())
        {
            headers.put(UUHttpHeader.Authorization, "$scheme $t")
        }
    }
}