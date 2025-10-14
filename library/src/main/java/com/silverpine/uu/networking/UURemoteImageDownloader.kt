package com.silverpine.uu.networking

import android.graphics.Bitmap
import com.silverpine.uu.core.UUResult

interface UURemoteImageDownloader
{
    suspend fun download(url: String): UUResult<Bitmap>
}