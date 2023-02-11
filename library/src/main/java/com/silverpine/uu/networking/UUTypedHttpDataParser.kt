package com.silverpine.uu.networking

interface UUTypedHttpDataParser<ResponseType>
{
    fun parse(data: ByteArray): ResponseType?
}
