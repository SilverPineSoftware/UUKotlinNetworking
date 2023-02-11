package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson

class UUJsonDataParser<ResponseType>(private val responseClass: Class<ResponseType>): UUHttpDataParser
{
    override fun parse(data: ByteArray): Any?
    {
        return UUJson.fromBytes(data, responseClass)
    }
}