package com.silverpine.uu.networking.test

import com.silverpine.uu.logging.UUConsoleLogWriter
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.UULogLevel
import com.silverpine.uu.logging.UULogger

object TestLogger
{
    fun init()
    {
        val writer = UUConsoleLogWriter()
        val logger = UULogger(writer)
        logger.logLevel = UULogLevel.VERBOSE
        UULog.setLogger(logger)
    }
}