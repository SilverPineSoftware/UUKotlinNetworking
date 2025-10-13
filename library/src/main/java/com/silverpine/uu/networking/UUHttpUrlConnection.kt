package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.logging.UULog
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection

fun HttpURLConnection.uuSetHeader(header: UUHttpHeader, value: String)
{
    setRequestProperty(header.key, value)
}

fun HttpURLConnection.uuSetHeaders(headers: UUHttpHeaders)
{
    headers.log("uuSetHeaders", "RequestHeaders")

    headers.forEach()
    { key, value ->
        setRequestProperty(key, value.joinToString(","))
    }
}

fun HttpURLConnection.uuWriteBody(body: ByteArray): UUError?
{
    var os: OutputStream? = null

    try
    {
        UULog.d(javaClass, "uuWriteBody", "RequestBody: ${body.uuUtf8().getOrNull()}")
        os = BufferedOutputStream(outputStream)
        os.write(body)
        os.flush()
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "uuWriteBody", "", ex)
        return UUHttpError.fromException(UUHttpErrorCode.WRITE_FAILED, ex)
    }
    finally
    {
        os?.uuSafeClose()
    }

    return null
}

fun HttpURLConnection.uuSafeDisconnect()
{
    try
    {
        disconnect()
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "uuSafeDisconnect", "", ex)
    }
}

suspend fun HttpURLConnection.uuHandleResponse(request: UUHttpRequest): UUHttpResponse
{
    try
    {
        return request.responseHandler.handleResponse(request, this)
    }
    catch (ex: Exception)
    {
        UULog.d(javaClass, "uuHandleResponse", "", ex)

        return UUHttpResponse(
            request = request,
            error = UUHttpError.fromException(UUHttpErrorCode.HandleResponseException, ex)
        )
    }
}