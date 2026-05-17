package com.silverpine.uu.networking

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

    suspend fun executeAuthorizedRequest(request: UUHttpRequest): UUHttpResponse
    {
        // 1) Renew authorization if needed
        val renewal = renewApiAuthorizationIfNeeded()
        renewal.error?.let()
        {
            // 2) If renewal fails, return immediately and do not attempt the original call
            return UUHttpResponse(request = request, error = it)
        }

        // 3) Execute the request
        var response = executeOneAuthorizedRequest(request)

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
                response = executeOneAuthorizedRequest(request)
            }
        }

        return response
    }

    /*fun executeAuthorizedRequest(
        request: UUHttpRequest,
        completion: UUObjectBlock<UUHttpResponse>,
    )
    {
        uuDispatch {
            val response = try
            {
                runBlocking { executeAuthorizedRequest(request) }
            }
            catch (ex: Exception)
            {
                UUHttpResponse(
                    request = request,
                    error = UUHttpError.fromException(UUHttpErrorCode.UNDEFINED, ex),
                )
            }
            uuDispatchMain { completion(response) }
        }
    }*/

    suspend fun executeOneAuthorizedRequest(
        request: UUHttpRequest,
    ): UUHttpResponse
    {
        if (request.authorizationProvider == null)
        {
            request.authorizationProvider = defaultAuthorizationProvider
        }

        return executeRequest(request)
    }

    /**
     * Executes a single request with no authorization added
     *
     * @param request the request
     * @return the response
     */
    suspend fun executeRequest(request: UUHttpRequest): UUHttpResponse
    {
        return session.executeRequest(request)
    }

    /*fun executeRequest(
        request: UUHttpRequest,
        completion: UUObjectBlock<UUHttpResponse>,
    )
    {
        uuDispatch {
            val response = try
            {
                runBlocking { executeRequest(request) }
            }
            catch (ex: Exception)
            {
                UUHttpResponse(
                    request = request,
                    error = UUHttpError.fromException(UUHttpErrorCode.UNDEFINED, ex),
                )
            }
            uuDispatchMain { completion(response) }
        }
    }*/

    // MARK: Public Overridable Methods

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
    open suspend fun shouldRenewApiAuthorization(error: com.silverpine.uu.core.UUError): Boolean
    {
        val errorCode = error.uuErrorCode() ?: return false
        return errorCode == UUHttpErrorCode.AUTHORIZATION_NEEDED
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
