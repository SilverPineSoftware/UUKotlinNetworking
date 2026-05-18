package com.silverpine.uu.networking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@ExtendWith(RobolectricExtension::class)
class UUNetworkErrorRobolectricTests
{
    @Test
    fun makeError_setsDescriptionAndResolution()
    {
        val error = UUNetworkError.makeError(UUNetworkErrorCode.TIMEOUT)

        assertEquals(UUNetworkErrorCode.TIMEOUT, error.uuNetworkErrorCode())
        assertEquals(UUNetworkErrorCode.TIMEOUT.errorDescription, error.errorDescription)
        assertEquals(UUNetworkErrorCode.TIMEOUT.errorResolution, error.errorResolution)
        assertNotNull(error.userInfo)
    }

    @Test
    fun makeError_fillsRequestFieldsInUserInfo()
    {
        val request = UUHttpRequest(url = "https://api.example.com/items", method = UUHttpMethod.POST)
        val error = UUNetworkError.makeError(UUNetworkErrorCode.HTTP_FAILURE, request)

        assertEquals("POST", error.userInfo?.getString(UUNetworkError.USER_INFO_KEY_HTTP_METHOD))
        assertEquals(request.toURL.toString(), error.userInfo?.getString(UUNetworkError.USER_INFO_KEY_REQUEST_URL))
    }

    @Test
    fun fromException_mapsSocketTimeoutToTimeout()
    {
        val error = UUNetworkError.fromException(
            UUNetworkErrorCode.UNDEFINED,
            SocketTimeoutException("timed out"),
            null,
        )

        assertEquals(UUNetworkErrorCode.TIMEOUT, error.uuNetworkErrorCode())
        assertEquals(SocketTimeoutException::class.java, error.exception?.javaClass)
    }

    @Test
    fun fromException_mapsUnknownHostToCannotFindHost()
    {
        val error = UUNetworkError.fromException(
            UUNetworkErrorCode.UNDEFINED,
            UnknownHostException("host"),
            null,
        )

        assertEquals(UUNetworkErrorCode.CANNOT_FIND_HOST, error.uuNetworkErrorCode())
    }

    @Test
    fun fromException_mapsSslFailuresToHttpError()
    {
        val error = UUNetworkError.fromException(
            UUNetworkErrorCode.UNDEFINED,
            SSLException("ssl"),
            null,
        )

        assertEquals(UUNetworkErrorCode.HTTP_ERROR, error.uuNetworkErrorCode())
    }

    @Test
    fun fromException_mapsSocketExceptionToHttpError()
    {
        val error = UUNetworkError.fromException(
            UUNetworkErrorCode.UNDEFINED,
            SocketException("socket"),
            null,
        )

        assertEquals(UUNetworkErrorCode.HTTP_ERROR, error.uuNetworkErrorCode())
    }

    @Test
    fun create_maps401ToAuthorizationNeededWithStatusCode()
    {
        val request = UUHttpRequest(url = "https://api.example.com/secure")
        val error = UUNetworkError.create(request, 401, """{"message":"unauthorized"}""")

        assertEquals(UUNetworkErrorCode.AUTHORIZATION_NEEDED, error.uuNetworkErrorCode())
        assertEquals(401, error.uuNetworkStatusCode())
        assertEquals("""{"message":"unauthorized"}""", error.uuNetworkAppResponse())
        assertEquals(request.toURL.toString(), error.uuNetworkRequestUrl())
    }
}
