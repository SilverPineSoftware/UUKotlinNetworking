package com.silverpine.uu.networking

import androidx.core.net.toUri
import com.silverpine.uu.core.uuFormatAsRfc3339
import com.silverpine.uu.networking.authorization.UUHttpAuthorizationProvider
import com.silverpine.uu.networking.connectivity.UUConnectivity
import com.silverpine.uu.networking.connectivity.UUConnectivityProvider
import java.net.CookieHandler
import java.net.Proxy
import java.net.URL
import java.util.UUID
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

typealias UUQueryStringsArgs = HashMap<String, String>
typealias UUPathArgs = ArrayList<String>

open class UUHttpRequest(
    val url: String,
    val path: UUPathArgs = arrayListOf(),
    val query: UUQueryStringsArgs = hashMapOf(),
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
    var connectivityProvider: UUConnectivityProvider? = UUConnectivity,
    var loggingMode: Array<UUHttpLoggingMode> = UUHttpLoggingMode.Info
)
{
    enum class State
    {
        Idle,
        CheckConnection,
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

    val toURL: URL
        get()
        {
            val builder = url.toUri().buildUpon()

            path.forEach()
            {
                builder.appendPath(it)
            }

            query.forEach()
            {
                builder.appendQueryParameter(it.key, it.value)
            }

            return URL(builder.build().toString())
        }
}



