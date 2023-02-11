package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson


class UUTypedJsonDataParser<ResponseType>(private val responseClass: Class<ResponseType>): UUTypedHttpDataParser<ResponseType>
{
    override fun parse(data: ByteArray): ResponseType?
    {
        return UUJson.fromBytes(data, responseClass)
    }
}
