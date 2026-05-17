package com.silverpine.uu.networking

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.networking.handlers.UUFileResponseHandler
import java.io.File

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

    override suspend fun download(url: String): UUResult<Bitmap, UUError>
    {
        val request = UUHttpRequest(url).apply()
        {
            method = UUHttpMethod.GET
            responseHandler = UUFileResponseHandler(downloadFolder)
        }

        val response = session.executeRequest(request)

        response.error?.let {
            return UUResult.failure(it)
        }

        val file = (response.parsedResponse as? File)
            ?: return failure(Error.DownloadFailed)

        val bmp = BitmapFactory.decodeFile(file.absolutePath)
            ?: return failure(Error.LoadBitmapFailed)

        return UUResult.success(bmp)
    }

    private fun failure(error: Error): UUResult<Bitmap, UUError>
    {
        return UUResult.failure(UUError(error.code, ERROR_DOMAIN))
    }
}
