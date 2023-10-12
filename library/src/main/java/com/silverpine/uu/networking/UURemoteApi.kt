package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuDispatch

open class UURemoteApi<ErrorType: Any>(
    private val errorClass: Class<ErrorType>,
    var session: UUHttpSession<ErrorType>,
    var authorizationProvider: UUHttpAuthorizationProvider? = null)
{
    private var isAuthorizingFlag: Boolean = false
    private var authorizeListeners: ArrayList<(Boolean, UUError?)->Unit> = arrayListOf()

    fun <ResponseType: Any> executeAuthorizedRequest(
        request: UUHttpRequest<ResponseType, ErrorType>,
        responseClass: Class<ResponseType>,
        completion: (UUHttpResponse<ResponseType, ErrorType>)->Unit)
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

            executeOneAuthorizedRequest(request, responseClass)
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
                                    executeOneAuthorizedRequest(request, responseClass, completion)
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
    fun <ResponseType: Any> executeOneAuthorizedRequest(
        request: UUHttpRequest<ResponseType, ErrorType>,
        responseClass: Class<ResponseType>,
        completion: (UUHttpResponse<ResponseType, ErrorType>)->Unit)
    {
        authorizationProvider?.attachAuthorization(request.headers)
        executeRequest(request, responseClass, completion)
    }

    /**
     * Executes a single request with no authorization added
     *
     * @param request the request
     * @param completion the callback
     */
    fun <ResponseType: Any> executeRequest(
        request: UUHttpRequest<ResponseType, ErrorType>,
        responseClass: Class<ResponseType>,
        completion: (UUHttpResponse<ResponseType, ErrorType>)->Unit)
    {
        request.responseParser =
            { data, contentType, contentEncoding ->
                parseSuccess(responseClass, data, contentType, contentEncoding)
            }

        request.errorParser = this::parseError
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
        val errorCode = error.uuErrorCode() ?: return false
        return (errorCode == UUHttpErrorCode.AUTHORIZATION_NEEDED)
    }

    open fun cancelAll()
    {
        session.cancelAll()
    }

    private fun <ResponseType: Any> parseSuccess(responseClass: Class<ResponseType>, data: ByteArray, contentType: String, contentEncoding: String): ResponseType?
    {
        return UUJson.fromBytes(data, responseClass)
    }

    protected open fun parseError(data: ByteArray, contentType: String, contentEncoding: String, httpCode: Int): ErrorType?
    {
        return UUJson.fromBytes(data, errorClass)
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
