package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.parsers.UUDownloadFileStreamParser
import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import java.io.File

open class UUFileResponseHandler(downloadFolder: File): UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUDownloadFileStreamParser(downloadFolder)
}