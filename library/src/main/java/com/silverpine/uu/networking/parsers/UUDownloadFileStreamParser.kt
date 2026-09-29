package com.silverpine.uu.networking.parsers

import com.silverpine.uu.core.uuCopyTo
import kotlinx.coroutines.CancellationException
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
     * @return success containing the [File] written under [downloadFolder], or failure on file creation,
     * copying, or closure. The output is closed on success and failure; the input remains open.
     * A failure may leave a partial file. Coroutine cancellation is rethrown.
     */
    override suspend fun parse(stream: InputStream, response: HttpURLConnection): Result<Any?>
    {
        return withContext(Dispatchers.IO)
        {
            try
            {
                val destFile = File(downloadFolder, File(response.url.path).name)
                FileOutputStream(destFile).use { output ->
                    stream.uuCopyTo(output).getOrThrow()
                }
                Result.success(destFile)
            }
            catch (ex: CancellationException)
            {
                throw ex
            }
            catch (ex: Exception)
            {
                Result.failure(ex)
            }
        }
    }
}
