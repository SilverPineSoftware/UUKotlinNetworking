package com.silverpine.uu.networking.connectivity

import android.content.Context
import com.silverpine.uu.core.UUError

object UUConnectivity: UUConnectivityProvider
{
    private var provider: UUConnectivityProvider? = null

    fun init(provider: UUConnectivityProvider)
    {
        this.provider = provider
    }

    override suspend fun checkConnection(): UUError?
    {
        return provider?.checkConnection()
    }
}

fun Context.uuNetworkConnectivityProvider(): UUConnectivityProvider
{
    return UUNetworkConnectivityProvider(this)
}