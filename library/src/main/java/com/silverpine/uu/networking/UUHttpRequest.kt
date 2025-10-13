package com.silverpine.uu.networking

import com.silverpine.uu.core.uuFormatAsRfc3339
import com.silverpine.uu.networking.authorization.UUHttpAuthorizationProvider
import java.net.CookieHandler
import java.net.Proxy
import java.util.UUID
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

open class UUHttpRequest(
    var uri: UUHttpUri,
    var method: UUHttpMethod = UUHttpMethod.GET,
    var headers: UUHttpHeaders = UUHttpHeaders(),
    var body: UUHttpBody? = null,
    val useCaches: Boolean = false,
    val defaultUseCaches: Boolean = false,
    val instanceFollowRedirects: Boolean = true,
    val cookieHandler: CookieHandler? = null,
    val connectTimeout: Int = 60_000,
    val readTimeout: Int = 60_000,
    var proxy: Proxy? = null,
    var socketFactory: SSLSocketFactory? = null,
    var hostNameVerifier: HostnameVerifier? = null,
    var authorizationProvider: UUHttpAuthorizationProvider? = null,
    var responseHandler: UUHttpResponseHandler = UUBaseResponseHandler(),
    var loggingMode: Array<UUHttpLoggingMode> = UUHttpLoggingMode.Info
)
{
    enum class State
    {
        Idle,
        OpenConnection,
        PrepareToSend,
        WriteRequest,
        PrepareToReceive,
        HandleResponse,
        Complete
    }

    val id = UUID.randomUUID().toString()

    var startTime: Long = 0
    var endTime: Long = 0
    var state: State = State.Idle

    fun start()
    {
        startTime = System.currentTimeMillis()
        UUHttpLogging.log(UUHttpLoggingMode.Request, this, "StartTime: ${startTime.uuFormatAsRfc3339()}")
    }

    fun end()
    {
        endTime = System.currentTimeMillis()
        UUHttpLogging.log(UUHttpLoggingMode.Response, this, "EndTime: ${endTime.uuFormatAsRfc3339()}")

        val durationSeconds = (endTime - startTime).toDouble() / 1000
        UUHttpLogging.log(UUHttpLoggingMode.Response, this, "Duration: $durationSeconds seconds")
    }
}



