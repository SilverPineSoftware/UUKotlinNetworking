package com.silverpine.uu.networking.handlers

import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUHttpError
import com.silverpine.uu.networking.UUHttpErrorCode
import com.silverpine.uu.networking.UUHttpLogging
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.parsers.UUBinaryStreamParser
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import com.silverpine.uu.networking.parsers.UUTypedStreamParser
import com.silverpine.uu.networking.uuIsHttpSuccess
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

interface UUHttpResponseHandler
{
    suspend fun handleResponse(
        request: UUHttpRequest,
        urlConnection: HttpURLConnection,
    ): UUHttpResponse

    val successParser: UUHttpStreamParser
    val errorParser: UUHttpStreamParser
}




