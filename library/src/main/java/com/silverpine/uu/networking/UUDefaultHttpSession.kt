package com.silverpine.uu.networking

import android.os.Parcelable
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuUtf8
import com.silverpine.uu.logging.UULog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.HttpURLConnection

open class UUDefaultHttpSession: UUHttpSession
{
    override fun executeRequest(request: UUHttpRequest, completion: (UUHttpResponse) -> Unit)
    {
        CoroutineScope(Dispatchers.IO).launch()
        {
            val response = executeRequestSync(request)
            completion.invoke(response)
        }
    }

    override fun cancelAll()
    {
    }

    private fun executeRequestSync(request: UUHttpRequest): UUHttpResponse
    {
        val response = UUHttpResponse(request)

        var urlConnection: HttpURLConnection? = null

        try
        {
            urlConnection = request.uuOpenConnection()
            if (urlConnection == null)
            {
                response.error = UUError(-2, "UUHttp.ERROR_DOMAIN")
                return response
            }

            request.startTime = System.currentTimeMillis()


//            if (urlConnection is HttpsURLConnection) {
//                val factory: SSLSocketFactory = request.getSocketFactory()
//                if (factory != null) {
//                    urlConnection.sslSocketFactory = factory
//                }
//            }

            UULog.d(javaClass, "executeRequest", "${request.method} ${urlConnection.url} ")
            UULog.d(javaClass, "executeRequest", "Timeout: ${request.timeout}")

            val requestBody = request.serializeBody()

            request.headers.log("executeRequest", "RequestHeaders")

            request.headers.forEach()
            { key, value ->
                urlConnection.setRequestProperty(key, value.joinToString(","))
            }

            if (requestBody != null)
            {
                urlConnection.doOutput = true
                urlConnection.setFixedLengthStreamingMode(requestBody.size)
                urlConnection.uuWrite(requestBody)
            }

            //urlConnection.uuWrite(request)

            response.httpCode = urlConnection.responseCode
            response.contentType = urlConnection.contentType ?: ""
            response.contentEncoding = urlConnection.contentEncoding ?: ""

            UULog.d(javaClass,"executeRequest", "HTTP Response Code: ${response.httpCode}")
            UULog.d(javaClass,"executeRequest", "Response Content-Type: ${response.contentType}")
            UULog.d(javaClass,"executeRequest", "Response Content-Encoding: ${response.contentEncoding}")

            response.headers.putAll(urlConnection.headerFields)

            response.headers.log("executeRequest", "ResponseHeader")

            response.rawResponse = urlConnection.uuReadResponse()
            response.rawResponse?.let()
            {
                UULog.d(javaClass, "executeRequest", "ResponseBody: ${String(it)}")

                if (response.httpCode.uuIsHttpSuccess())
                {
                    response.parsedResponse = request.responseParser?.parse(it)
                }
                else
                {
                    val parsedError = request.responseParser?.parse(it)
                    var responseCode = UUHttpErrorCode.HTTP_ERROR

                    if (response.httpCode == 401)
                    {
                        responseCode = UUHttpErrorCode.AUTHORIZATION_NEEDED
                    }

                    response.error = UUError(responseCode.value, "UUHttp.ERROR_DOMAIN", userInfo = parsedError as? Parcelable)

                }

            } ?: run()
            {
                UULog.d(javaClass, "executeRequest", "ResponseBody: NULL")
            }
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "executeRequest", "", ex)
            response.exception = ex
        }
        finally
        {
            urlConnection?.uuSafeDisconnect()
        }

        return response
    }

    private fun UUHttpRequest.uuOpenConnection(): HttpURLConnection?
    {
        val url = uri.toURL() ?: return null

        val urlConnection = if (proxy != null)
        {
            url.openConnection(proxy) as? HttpURLConnection
        }
        else
        {
            url.openConnection() as? HttpURLConnection
        }

        urlConnection?.connectTimeout = timeout
        urlConnection?.readTimeout = timeout
        urlConnection?.doInput = true
        urlConnection?.requestMethod = method.toString()

        if (useGZipCompression)
        {
            urlConnection?.setRequestProperty("Accept-Encoding", "gzip")
        }

        return urlConnection
    }

    private fun HttpURLConnection.uuWrite(body: ByteArray)
    {
        var os: OutputStream? = null

        try
        {
            UULog.d(javaClass, "executeRequest", "RequestBody: ${body.uuUtf8()}")
            os = BufferedOutputStream(outputStream)
            os.write(body)
            os.flush()
        }
        catch (ex: IOException)
        {
            UULog.d(javaClass, "writeRequest", "", ex)
        }
        finally
        {
            os?.uuSafeClose()
        }
    }

    private fun Closeable?.uuSafeClose()
    {
        try
        {
            this?.close()
        }
        catch (_: Exception)
        {
            // Eat it
        }
    }

    private fun HttpURLConnection?.uuSafeDisconnect()
    {
        try
        {
            this?.disconnect()
        }
        catch (_: Exception)
        {
            // Eat it
        }
    }

    private fun HttpURLConnection.uuReadResponse(): ByteArray?
    {
        try
        {
            val readStream = if (responseCode.uuIsHttpSuccess())
            {
                inputStream
            }
            else
            {
                errorStream
            }

            var bytesRead: Int
            val buffer = ByteArray(10240)
            val bos = ByteArrayOutputStream()

            while (true)
            {
                bytesRead = readStream.read(buffer, 0, buffer.size)
                if (bytesRead == -1)
                {
                    break
                }

                bos.write(buffer, 0, bytesRead)
            }

            return bos.toByteArray()
        }
        catch (ex: Exception)
        {
            return null
        }
    }
}