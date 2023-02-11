package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

class UURemoteApiResponse<ResponseType>
{
    var httpCode: Int = 0
    var headers: Map<String, List<String>>? = null
    var caughtException: Exception? = null
    var data: ResponseType? = null
    var error: UUError? = null

    val wasSuccessful: Boolean
        get()
        {
            return httpCode in 200.. 299
        }

}