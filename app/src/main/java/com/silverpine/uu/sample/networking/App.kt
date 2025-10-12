package com.silverpine.uu.sample.networking

import android.app.Application
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.UUKotlinXJsonProvider
import com.silverpine.uu.core.security.UUSecurePrefs
import com.silverpine.uu.logging.UUConsoleLogger
import com.silverpine.uu.logging.UULog
import kotlinx.serialization.json.Json

class App: Application()
{
    override fun onCreate()
    {
        super.onCreate()
        setupUU()
    }

    fun setupUU()
    {
        UULog.init(UUConsoleLogger())

        UUJson.init(
            UUKotlinXJsonProvider(Json()
            {
                ignoreUnknownKeys = true
            }))

        UUSecurePrefs.init(applicationContext)
    }
}