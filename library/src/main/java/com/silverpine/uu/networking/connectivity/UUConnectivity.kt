package com.silverpine.uu.networking.connectivity

import android.content.Context
import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.connectivity.UUConnectivity.checkConnection
import com.silverpine.uu.networking.connectivity.UUConnectivity.init

/**
 * Application-wide delegate for [UUConnectivityProvider].
 *
 * [UUHttpRequest] uses this object as its default [UUHttpRequest.connectivityProvider].
 * Call [init] once at startup (typically from `Application.onCreate`) with a platform
 * implementation such as [UUNetworkConnectivityProvider]. Until [init] runs,
 * [checkConnection] returns `null` (connectivity is treated as unknown, not offline).
 *
 * ### Typical usage
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         UUConnectivity.init(uuNetworkConnectivityProvider())
 *     }
 * }
 * ```
 *
 * @see UUConnectivityProvider
 * @see UUNetworkConnectivityProvider
 * @see UUHttpRequest.connectivityProvider
 */
object UUConnectivity : UUConnectivityProvider
{
    private var provider: UUConnectivityProvider? = null

    /**
     * Registers the [UUConnectivityProvider] used for all requests that rely on the default
     * [UUConnectivity] delegate.
     *
     * @param provider implementation that performs reachability checks; usually
     * [UUNetworkConnectivityProvider] on Android.
     */
    fun init(provider: UUConnectivityProvider)
    {
        this.provider = provider
    }

    /**
     * Forwards to the provider passed to [init], or returns `null` if none was registered.
     *
     * @return a [UUError] when the registered provider reports no usable connection,
     * or `null` when connected or not yet configured.
     */
    override suspend fun checkConnection(): UUError?
    {
        return provider?.checkConnection()
    }
}

/**
 * Creates a [UUNetworkConnectivityProvider] for this [Context].
 *
 * Convenience for `UUConnectivity.init(UUNetworkConnectivityProvider(context))` in
 * `Application.onCreate`.
 *
 * @see UUNetworkConnectivityProvider
 * @see UUConnectivity.init
 */
fun Context.uuNetworkConnectivityProvider(): UUConnectivityProvider
{
    return UUNetworkConnectivityProvider(this)
}