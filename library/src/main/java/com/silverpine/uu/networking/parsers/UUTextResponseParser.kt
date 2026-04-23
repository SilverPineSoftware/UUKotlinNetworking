package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuReadAll
import java.io.InputStream
import java.net.HttpURLConnection

class UUTextResponseParser: UUHttpStreamParser
{
    override fun parse(stream: InputStream, response: HttpURLConnection): Any?
    {
        //response.contentType
        //response.contentEncoding

        // Encoding and Charset
        val bytes = stream.uuReadAll() ?: return null
        return String(bytes)
    }
}