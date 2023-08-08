package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.uuDispatch

open class UURemoteApi<ErrorType>(
    var session: UUHttpSession<ErrorType>,
    var authorizationProvider: UUHttpAuthorizationProvider? = null)
{
    private var isAuthorizingFlag: Boolean = false
    private var authorizeListeners: ArrayList<(Boolean, UUError?)->Unit> = arrayListOf()

    fun <ResponseType> executeRequest(request: UUHttpRequest<ResponseType, ErrorType>, completion: (UUHttpResponse<ResponseType, ErrorType>)->Unit)
    {
        renewApiAuthorizationIfNeeded()
        { _, authorizationRenewalError ->

            if (authorizationRenewalError != null)
            {
                val response = UUHttpResponse(request)
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
                        { didAttempt, innerAuthorizationRenewalError ->

                            innerAuthorizationRenewalError?.let()
                            {
                                val errorResponse = UUHttpResponse(request)
                                errorResponse.error = innerAuthorizationRenewalError
                                completion(errorResponse)
                            } ?: run()
                            {
                                if (didAttempt)
                                {
                                    executeOneRequest(request, completion)
                                }
                                else
                                {
                                    completion(response)
                                }
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
    fun <ResponseType> executeOneRequest(request: UUHttpRequest<ResponseType, ErrorType>, completion: (UUHttpResponse<ResponseType, ErrorType>)->Unit)
    {
        authorizationProvider?.attachAuthorization(request.headers)
        session.executeRequest(request, completion)
    }

    // MARK: Public Overridable Methods

    /**
    Perform an api authorization/renewal.  Typically this means fetching a JWT from a server,.

    Default behavior is to just return nil
     */
    open fun renewApiAuthorization(completion: (Boolean,UUError?)->Unit)
    {
        completion(false, null)
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

    private fun renewApiAuthorizationIfNeeded(completion: (Boolean, UUError?)->Unit)
    {
        isApiAuthorizationNeeded()
        { authorizationNeeded ->

            if (authorizationNeeded)
            {
                internalRenewApiAuthorization(completion)
            }
            else
            {
                completion(false, null)
            }
        }
    }

    private fun internalRenewApiAuthorization(completion: (Boolean, UUError?)->Unit)
    {
        addAuthorizeListener(completion)

        val isAuthorizing = isAuthorizing()

        if (isAuthorizing)
        {
            return
        }

        setAuthorizing(true)

        renewApiAuthorization()
        { didAttempt, error ->
            notifyAuthorizeListeners(didAttempt, error)
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
    private fun addAuthorizeListener(listener: (Boolean,UUError?)->Unit)
    {
        authorizeListeners.add(listener)
    }

    @Synchronized
    private fun notifyAuthorizeListeners(didAttempt: Boolean, error: UUError?)
    {
        val listenersToNotify: ArrayList<(Boolean, UUError?)->Unit> = arrayListOf()
        listenersToNotify.addAll(authorizeListeners)
        authorizeListeners.clear()

        // At this point, authorize is done, we have the response error,
        // and if a new call to authorize comes in, we want it allow it
        setAuthorizing(false)

        listenersToNotify.forEach()
        { listener ->

            uuDispatch()
            {
                listener(didAttempt, error)
            }
        }
    }
}
