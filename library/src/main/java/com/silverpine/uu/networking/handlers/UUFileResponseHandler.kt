package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.parsers.UUBinaryStreamParser
import com.silverpine.uu.networking.parsers.UUDownloadFileStreamParser
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import java.io.File

/**
 * [UUBaseResponseHandler] that writes successful response bodies to disk.
 *
 * [successParser] is [UUDownloadFileStreamParser], which saves the stream to a file under
 * [downloadFolder] using the last path segment of the response URL as the file name.
 * [errorParser] remains the default [UUBinaryStreamParser].
 *
 * ### Example
 * ```kotlin
 * request.responseHandler = UUFileResponseHandler(context.cacheDir)
 * ```
 *
 * @param downloadFolder directory that receives downloaded files.
 * @see UUDownloadFileStreamParser
 * @see UUBaseResponseHandler
 */
open class UUFileResponseHandler(downloadFolder: File) : UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUDownloadFileStreamParser(downloadFolder)
}
