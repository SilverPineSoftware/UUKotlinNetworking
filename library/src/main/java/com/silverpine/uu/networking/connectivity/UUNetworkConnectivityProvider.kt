package com.silverpine.uu.networking.connectivity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.UUNetworkError
import com.silverpine.uu.networking.UUNetworkErrorCode

/**
 * Default implementation of [UUConnectivityProvider] that uses Android's
 * [ConnectivityManager] and [NetworkCapabilities] APIs to determine
 * the device’s current network connectivity state.
 *
 * This provider checks for:
 *  - **Captive portals** (e.g., hotel or public Wi-Fi login pages) and
 *    returns a [UUNetworkError] with [UUNetworkErrorCode.CAPTIVE_NETWORK_LOGIN_NEEDED].
 *  - **Lack of internet validation** (no route to external network),
 *    returning a [UUNetworkError] with [UUNetworkErrorCode.NO_INTERNET].
 *
 * If the device appears connected and validated, `checkConnection()` returns `null`.
 * This allows callers to proceed with network operations without interruption.
 *
 * ### Permission Requirements
 * Requires the `android.permission.ACCESS_NETWORK_STATE` permission.
 * If the permission is missing, all checks will safely fail and return `null`
 * (indicating "unknown" rather than "offline").
 *
 * ### Typical Usage
 * ```kotlin
 * val provider = UUNetworkConnectivityProvider(context)
 * val error = provider.checkConnection()
 * if (error != null) {
 *     // Handle connectivity error, e.g. show offline message or defer request
 * }
 * ```
 *
 * @property context Android [Context] used to obtain the [ConnectivityManager].
 * @see UUConnectivityProvider
 * @see UUNetworkError
 * @see UUNetworkErrorCode
 */
class UUNetworkConnectivityProvider(val context: Context) : UUConnectivityProvider
{
    /**
     * Checks the current network state for connectivity and validation.
     *
     * @return a [UUError] representing a connectivity problem, or `null` if the
     *         network is connected and validated.
     */
    override suspend fun checkConnection(): UUError?
    {
        val caps = getNetworkCapabilities() ?: return null

        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL))
        {
            val error = UUNetworkError.makeError(UUNetworkErrorCode.CAPTIVE_NETWORK_LOGIN_NEEDED)
            return error
        }

        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        {
            val error = UUNetworkError.makeError(UUNetworkErrorCode.NO_INTERNET)
            return error
        }

        return null
    }

    /**
     * Retrieves the current [NetworkCapabilities] for the active network.
     *
     * Returns `null` if the `ACCESS_NETWORK_STATE` permission is missing,
     * or if no active network is available.
     */
    @SuppressLint("MissingPermission")
    private fun getNetworkCapabilities(): NetworkCapabilities?
    {
        if (!hasAccessNetworkStatePermission())
        {
            return null
        }

        val cm: ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork
        return cm.getNetworkCapabilities(network)
    }

    /**
     * Checks whether the app has the `ACCESS_NETWORK_STATE` permission.
     *
     * @return `true` if permission is granted, otherwise `false`.
     */
    private fun hasAccessNetworkStatePermission(): Boolean
    {
        val result = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }
}
