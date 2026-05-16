package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpResponse
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import java.net.HttpURLConnection

interface UUHttpResponseHandler
{
    suspend fun handleResponse(
        request: UUHttpRequest,
        urlConnection: HttpURLConnection,
    ): UUHttpResponse

    val successParser: UUHttpStreamParser
    val errorParser: UUHttpStreamParser
}




