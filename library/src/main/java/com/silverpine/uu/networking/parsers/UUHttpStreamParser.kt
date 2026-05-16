package com.silverpine.uu.networking.parsers

import java.io.InputStream
import java.net.HttpURLConnection

fun interface UUHttpStreamParser
{
    fun parse(
        stream: InputStream,
        response: HttpURLConnection
    ): Any?
}