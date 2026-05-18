package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.UUJson
import java.io.InputStream
import java.net.HttpURLConnection

open class UUTypedStreamParser<DataType: Any>(private val objectClass: Class<DataType>): UUHttpStreamParser
{
    override suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection
    ): Any?
    {
        return UUJson.fromStream(stream, objectClass).getOrNull()
    }
}
