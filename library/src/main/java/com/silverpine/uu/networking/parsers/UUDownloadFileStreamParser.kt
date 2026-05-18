package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuCopyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * [UUHttpStreamParser] that writes the response body to a file under [downloadFolder].
 *
 * The destination file name is taken from the last segment of [HttpURLConnection.url]
 * ([java.net.URL.getPath] → [File.getName]). Existing files with the same name are overwritten.
 *
 * Used by [com.silverpine.uu.networking.handlers.UUFileResponseHandler]. For zip extraction or
 * custom naming, supply a parser via [uuHttpStreamParser] instead.
 *
 * @property downloadFolder directory that receives downloaded files; must exist and be writable.
 * @see com.silverpine.uu.networking.handlers.UUFileResponseHandler
 * @see uuHttpStreamParser
 */
class UUDownloadFileStreamParser(val downloadFolder: File) : UUHttpStreamParser
{
    /**
     * @return the [File] written under [downloadFolder], or `null` only if stream setup fails before write.
     *   Copy errors are logged by [com.silverpine.uu.core.uuCopyTo]; a partial or empty file may still be returned.
     */
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
