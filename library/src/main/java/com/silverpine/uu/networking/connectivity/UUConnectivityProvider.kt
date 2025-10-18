package com.silverpine.uu.networking.connectivity

import com.silverpine.uu.core.UUError

/**
 * Defines a simple interface for performing network or connectivity checks.
 *
 * Implementations of this interface can be plugged into components that need
 * to verify reachability before attempting network operations.
 *
 * The design allows each implementation to emit a custom [UUError] to represent
 * connectivity issues in a consistent, framework-agnostic way.
 *
 * @return a [UUError] describing the connectivity failure, or `null`
 * if the device is considered online and ready for network operations.
 *
 * @see UUError
 */
interface UUConnectivityProvider
{
    /**
     * Performs a synchronous connectivity check.
     *
     * @return a [UUError] if no connection is available, or `null` if connected.
     */
    fun checkConnection(): UUError?
}