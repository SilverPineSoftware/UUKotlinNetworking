package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSubData
import com.silverpine.uu.logging.UULog
import java.io.BufferedInputStream
import kotlin.math.min

private const val LOG_TAG = "UUHttp"

object UUHttpLogging
{
    fun log(
        mode: UUHttpLoggingMode,
        request: UUHttpRequest,
        message: String,
        throwable: Throwable? = null)
    {
        if (request.loggingMode.contains(mode))
        {
            val throwablePart = throwable?.let { ", $it" } ?: ""
            UULog.debug(LOG_TAG, "UUHttpLogging [${request.id}] [${mode.name}]- ${message}${throwablePart}")
        }
    }

    fun logHeaders(
        request: UUHttpRequest,
        mode: UUHttpLoggingMode,
        headers: UUHttpHeaders)
    {
        if (request.loggingMode.contains(mode))
        {
            for (entry in headers.entries)
            {
                log(mode, request, "${entry.key}=${entry.value.joinToString(",")}")
            }
        }
    }

    fun logError(
        request: UUHttpRequest,
        error: UUError)
    {
        log(UUHttpLoggingMode.Errors, request, "state=${request.state} error=$error", null)
    }

    fun logRequest(request: UUHttpRequest, bytes: ByteArray)
    {
        if (request.loggingMode.contains(UUHttpLoggingMode.RequestBody))
        {
            val peekSize = 10_000 // or make this configurable
            val string = bytes.uuSubData(0, min(bytes.size, peekSize))?.toString(Charsets.UTF_8)

            log(UUHttpLoggingMode.RequestBody, request, string ?: "")
        }
    }

    fun logResponse(request: UUHttpRequest, responseStream: BufferedInputStream)
    {
        if (request.loggingMode.contains(UUHttpLoggingMode.ResponseBody))
        {
            val peekSize = 10_000 // or make this configurable
            responseStream.mark(peekSize + 1)

            val peekBuffer = ByteArray(peekSize)
            val previewBytesRead = responseStream.read(peekBuffer, 0, peekBuffer.size)

            val previewString = peekBuffer.uuSubData(0, previewBytesRead)?.toString(Charsets.UTF_8)

            log(UUHttpLoggingMode.ResponseBody, request, previewString ?: "")

            responseStream.reset()
        }
    }
}