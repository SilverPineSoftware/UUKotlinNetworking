package com.silverpine.uu.networking

import java.io.File

open class UUFileResponseHandler(downloadFolder: File): UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUDownloadFileStreamParser(downloadFolder)
}