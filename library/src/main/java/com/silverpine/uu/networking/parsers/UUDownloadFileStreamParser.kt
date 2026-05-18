package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuCopyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection

class UUDownloadFileStreamParser(val downloadFolder: File): UUHttpStreamParser
{
    override suspend fun parse(stream: InputStream, response: HttpURLConnection): Any?
    {
        val fileName = File(response.url.path).name
        val destFile = File(downloadFolder, fileName)
        val fos = withContext(Dispatchers.IO)
        {
            FileOutputStream(destFile)
        }

        stream.uuCopyTo(fos)
        return destFile
    }
}