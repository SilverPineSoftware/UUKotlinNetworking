package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.authorization.UUHttpAuthorizationProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class UURemoteApi(
    var session: UUHttpSession = UUHttpSession(),
)
{
    // AuthorizationProvider used if none is specified on a request
    var defaultAuthorizationProvider: UUHttpAuthorizationProvider? = null

    private val renewAuthorizationMutex = Mutex()
    private var renewAuthorizationInFlight: Deferred<UURenewAuthorizationResponse>? = null

    open suspend fun execute(request: UUHttpRequest): UUHttpResponse
    {
        // 1) Renew authorization if needed
        val renewal = renewApiAuthorizationIfNeeded()
        renewal.error?.let()
        {
            // 2) If renewal fails, return immediately and do not attempt the original call
            return UUHttpResponse(request = request, error = it)
        }

        // 3) Execute the request
        var response = executeWithoutAuthorizationRenewal(request)

        // 4) If an error is returned and that error indicates an authorization
        // renewal is warranted, then attempt authorization renewal again.
        val error = response.error
        if (error != null && shouldRenewApiAuthorization(error))
        {
            // 5) Try an authorization renewal
            val retryRenewal = internalRenewApiAuthorization()
            retryRenewal.error?.let()
            {
                // 6) Immediately return on error
                return UUHttpResponse(request = request, error = it)
            }

            // 7) If authorization renewal was attempted, then retry the request
            if (retryRenewal.didAttempt)
            {
                response = executeWithoutAuthorizationRenewal(request)
            }
        }

        return response
    }

    open suspend fun prepareRequest(request: UUHttpRequest)
    {
        if (request.authorizationProvider == null)
        {
            request.authorizationProvider = defaultAuthorizationProvider
        }
    }

    open suspend fun executeWithoutAuthorizationRenewal(
        request: UUHttpRequest,
    ): UUHttpResponse
    {
        prepareRequest(request)
        return session.execute(request)
    }

    /**
     * Perform an api authorization/renewal. Typically, this means fetching a JWT from a server.
     *
     * Default behavior is to return didAttempt = false with no error.
     */
    open suspend fun renewApiAuthorization(): UURenewAuthorizationResponse
    {
        return UURenewAuthorizationResponse(false, null)
    }

    /**
     * Returns whether api authorization is needed ahead of making an actual api call.
     * Typically, this means checking a JWT expiration.
     *
     * Default behavior is to return false.
     */
    open suspend fun isApiAuthorizationNeeded(): Boolean
    {
        return false
    }

    /**
     * Determines if api authorization is needed based on an Error.
     *
     * Default behavior is to return true if the error code is authorizationNeeded.
     */
    open suspend fun shouldRenewApiAuthorization(error: UUError): Boolean
    {
        //return error.httpErrorCode == UUHttpErrorCode.AUTHORIZATION_NEEDED

        val errorCode = error.uuNetworkErrorCode() ?: return false
        return errorCode == UUNetworkErrorCode.AUTHORIZATION_NEEDED
    }

    open suspend fun cancelAll()
    {
        session.cancelAll()
    }

    // MARK: Private Implementation

    private suspend fun renewApiAuthorizationIfNeeded(): UURenewAuthorizationResponse
    {
        return if (isApiAuthorizationNeeded())
        {
            internalRenewApiAuthorization()
        }
        else
        {
            UURenewAuthorizationResponse(false, null)
        }
    }

    /**
     * Coalesces concurrent renewal attempts: one [renewApiAuthorization] runs; other callers
     * await the same [Deferred] result.
     */
    private suspend fun internalRenewApiAuthorization(): UURenewAuthorizationResponse =
        coroutineScope()
        {
            // 1) If there is an active renewal happening, await it
            renewAuthorizationInFlight?.takeIf()
            {
                it.isActive
            }?.let()
            {
                return@coroutineScope it.await()
            }

            // 2) Prepare an async block that will do the authorization renewal
            val deferred = renewAuthorizationMutex.withLock()
            {
                renewAuthorizationInFlight?.takeIf()
                { it.isActive
                }?.let()
                {
                    return@withLock it
                }

                async()
                {
                    renewApiAuthorization()
                }.also()
                {
                    renewAuthorizationInFlight = it
                }
            }

            // 3) Await the renewal block
            try
            {
                deferred.await()
            }
            finally
            {
                // 4) Clear the mutex
                renewAuthorizationMutex.withLock()
                {
                    if (renewAuthorizationInFlight === deferred)
                    {
                        renewAuthorizationInFlight = null
                    }
                }
            }
        }
}
