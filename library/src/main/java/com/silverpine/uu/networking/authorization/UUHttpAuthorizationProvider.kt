package com.silverpine.uu.networking.authorization

import com.silverpine.uu.networking.UUHttpHeader
import com.silverpine.uu.networking.UUHttpRequest

open class UUHttpAuthorizationProvider(
    var scheme: String,
    var authorization: String?
)
{
    open fun formatAuthorization(): String?
    {
        return authorization
    }

    open fun attachAuthorization(request: UUHttpRequest)
    {
        val t = formatAuthorization() ?: return
        if (t.isNotEmpty())
        {
            request.headers.put(UUHttpHeader.Authorization, "$scheme $t")
        }
    }
}