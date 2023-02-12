package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUThread

open class UUTypedRemoteApi<ErrorType>(
    var session: UUTypedHttpSession<ErrorType>,
    var authorizationProvider: UUHttpAuthorizationProvider? = null)
{
    private var isAuthorizingFlag: Boolean = false
    private var authorizeListeners: ArrayList<(UUError?)->Unit> = arrayListOf()

    fun <ResponseType> executeRequest(request: UUTypedHttpRequest<ResponseType, ErrorType>, completion: (UUTypedHttpResponse<ResponseType, ErrorType>)->Unit)
    {
        renewApiAuthorizationIfNeeded()
        { authorizationRenewalError ->

            if (authorizationRenewalError != null)
            {
                val response = UUTypedHttpResponse(request)
                response.error = authorizationRenewalError
                completion(response)
                return@renewApiAuthorizationIfNeeded
            }

            executeOneRequest(request)
            { response ->

                response.error?.let()
                { err ->

                    if (shouldRenewApiAuthorization(err))
                    {
                        internalRenewApiAuthorization()
                        { innerAuthorizationRenewalError ->

                            innerAuthorizationRenewalError?.let()
                            {
                                val errorResponse = UUTypedHttpResponse(request)
                                errorResponse.error = innerAuthorizationRenewalError
                                completion(errorResponse)
                            } ?: run()
                            {
                                executeOneRequest(request, completion)
                            }
                        }
                    }
                    else
                    {
                        completion(response)
                    }

                } ?: run()
                {
                    completion(response)
                }
            }
        }
    }

    /**
    Executes a single request with no api authorization checks
     */
    fun <ResponseType> executeOneRequest(request: UUTypedHttpRequest<ResponseType, ErrorType>, completion: (UUTypedHttpResponse<ResponseType, ErrorType>)->Unit)
    {
        authorizationProvider?.attachAuthorization(request.headers)
        session.executeRequest(request, completion)
    }

    // MARK: Public Overridable Methods

    /**
    Perform an api authorization/renewal.  Typically this means fetching a JWT from a server,.

    Default behavior is to just return nil
     */
    open fun renewApiAuthorization(completion: (UUError?)->Unit)
    {
        completion(null)
    }

    /**
    Returns whether api authorization is needed ahead of making an actual api call.  Typically this means checking a JWT expiration

    Default behavior is to return false
     */
    open fun isApiAuthorizationNeeded(completion: (Boolean)->Unit)
    {
        completion(false)
    }

    /**
    Determines if api authorization is needed based on an Error

    Default behavior is to return  true if the UUHttpSessionError is authorizationNeeded.
     */
    open fun shouldRenewApiAuthorization(error: UUError): Boolean
    {
        val errorCode = error.uuHttpErrorCode() ?: return false
        return (errorCode == UUHttpErrorCode.AUTHORIZATION_NEEDED)
    }

    open fun cancelAll()
    {
        session.cancelAll()
    }

    // MARK: Private Implementation

    private fun renewApiAuthorizationIfNeeded(completion: (UUError?)->Unit)
    {
        isApiAuthorizationNeeded()
        { authorizationNeeded ->

            if (authorizationNeeded)
            {
                internalRenewApiAuthorization(completion)
            }
            else
            {
                completion(null)
            }
        }
    }

    private fun internalRenewApiAuthorization(completion: (UUError?)->Unit)
    {
        addAuthorizeListener(completion)

        val isAuthorizing = isAuthorizing()

        if (isAuthorizing)
        {
            return
        }

        setAuthorizing(true)

        renewApiAuthorization()
        { error ->
            notifyAuthorizeListeners(error)
        }
    }

    @Synchronized
    private fun setAuthorizing(value: Boolean)
    {
        isAuthorizingFlag = value
    }

    @Synchronized
    private fun isAuthorizing(): Boolean
    {
        return isAuthorizingFlag
    }

    @Synchronized
    private fun addAuthorizeListener(listener: (UUError?)->Unit)
    {
        authorizeListeners.add(listener)
    }

    @Synchronized
    private fun notifyAuthorizeListeners(error: UUError?)
    {
        val listenersToNotify: ArrayList<(UUError?)->Unit> = arrayListOf()
        listenersToNotify.addAll(authorizeListeners)
        authorizeListeners.clear()

        // At this point, authorize is done, we have the response error,
        // and if a new call to authorize comes in, we want it allow it
        setAuthorizing(false)

        listenersToNotify.forEach()
        { listener ->

            UUThread.runOnBackgroundThread()
            {
                listener(error)
            }
        }
    }
}
