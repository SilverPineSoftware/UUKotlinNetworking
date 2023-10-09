package com.silverpine.uu.networking

import com.silverpine.uu.core.uuFromJson

inline fun <reified T> uuParseJsonResponse(data: ByteArray, contentType: String, contentEncoding: String): T?
{
    if (contentType != UUContentType.APPLICATION_JSON)
    {
        return null
    }

    return data.uuFromJson()
}