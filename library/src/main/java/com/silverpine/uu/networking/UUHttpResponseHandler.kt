package com.silverpine.uu.networking

import android.provider.ContactsContract.Data

interface UUHttpResponseHandler
{
    fun handleResponse(request: UUHttpRequest, data: Data?, response: Any?, error: Error?, completion: (UUHttpResponse)->Unit)
    val dataParser: UUHttpDataParser
}

open class UUBaseResponseHandler: UUHttpResponseHandler
{
    override fun handleResponse(
        request: UUHttpRequest,
        data: Data?,
        response: Any?,
        error: Error?,
        completion: (UUHttpResponse) -> Unit
    )
    {
    }

    override val dataParser: UUHttpDataParser
        get() = TODO("Not yet implemented")

}