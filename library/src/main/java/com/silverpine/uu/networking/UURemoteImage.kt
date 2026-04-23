package com.silverpine.uu.networking

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.networking.handlers.UUFileResponseHandler
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class UURemoteImage(
    val session: UUHttpSession = UUHttpSession(),
    val downloadFolder: File): UURemoteImageDownloader
{
    companion object
    {
        const val ERROR_DOMAIN = "UURemoteImage"
    }

    enum class Error(val code: Int)
    {
        DownloadFailed(1000),
        LoadBitmapFailed(1001)
    }

    override suspend fun download(url: String): UUResult<Bitmap>
    {
        return suspendCoroutine()
        { continuation ->

            val request = UUHttpRequest(url).apply()
            {
                method = UUHttpMethod.GET
                responseHandler = UUFileResponseHandler(downloadFolder)
            }

            session.executeRequest(request)
            { response ->

                response.error?.let()
                {
                    continuation.resume(UUResult.failure(it))
                    return@executeRequest
                }

                val file = (response.parsedResponse as? File)
                if (file == null)
                {
                    continuation.resume(failure(Error.DownloadFailed))
                    return@executeRequest
                }

                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp == null)
                {
                    continuation.resume(failure(Error.LoadBitmapFailed))
                    return@executeRequest
                }

                continuation.resume(UUResult.success(bmp))
            }
        }
    }

    private fun failure(error: Error): UUResult<Bitmap>
    {
        return UUResult.failure(UUError(error.code, ERROR_DOMAIN))
    }
}