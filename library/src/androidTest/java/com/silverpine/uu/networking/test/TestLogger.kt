package com.silverpine.uu.networking.test

import com.silverpine.uu.logging.UUConsoleLogWriter
import com.silverpine.uu.logging.UULog
import com.silverpine.uu.logging.UULogger

object TestLogger
{
    fun init()
    {
        UULog.setLogger(UULogger(UUConsoleLogWriter()))
    }
}