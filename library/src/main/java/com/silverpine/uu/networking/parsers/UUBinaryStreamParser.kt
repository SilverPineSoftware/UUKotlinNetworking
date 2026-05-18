package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuReadAll
import java.io.InputStream
import java.net.HttpURLConnection

open class UUBinaryStreamParser: UUHttpStreamParser
{
    override suspend fun parse(
        stream: InputStream,
        response: HttpURLConnection
    ): Any?
    {
        return stream.uuReadAll()
    }
}