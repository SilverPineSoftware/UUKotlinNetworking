package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuSafeClose
import com.silverpine.uu.logging.UULog
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

fun HttpURLConnection.uuSetHeaders(headers: UUHttpHeaders)
{
    headers.forEach()
    { key, value ->
        setRequestProperty(key, value.joinToString(","))
    }
}

fun URL.uuOpenConnection(proxy: Proxy? = null): HttpURLConnection?
{
    return if (proxy != null)
    {
        openConnection(proxy) as? HttpURLConnection
    }
    else
    {
        openConnection() as? HttpURLConnection
    }
}

fun HttpURLConnection.uuWriteBody(body: ByteArray): UUError?
{
    var os: OutputStream? = null

    try
    {
        os = BufferedOutputStream(outputStream)
        os.write(body)
        os.flush()
    }
    catch (ex: Exception)
    {
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