package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import com.silverpine.uu.networking.connectivity.UUConnectivityProvider
import com.silverpine.uu.test.UUAssert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockStatic
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.net.URL
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi


class UUHttpSessionTests
{
    /*
    data class Case(
        val name: String,
        val thrown: Throwable,
        val expectedCode: UUHttpErrorCode
    )

    companion object {
        @JvmStatic
        fun exceptionCases() = listOf(
            Case(
                name = "Socket timeout -> TIMEOUT",
                thrown = SocketTimeoutException("read timed out"),
                expectedCode = UUHttpErrorCode.TIMEOUT  // <-- adjust if named differently
            ),
            Case(
                name = "Unknown host -> HOST_NOT_FOUND",
                thrown = UnknownHostException("no such host"),
                expectedCode = UUHttpErrorCode.HOST_NOT_FOUND // <-- adjust name if needed
            ),
            Case(
                name = "Connect refused -> CONNECTION_FAILED",
                thrown = ConnectException("Connection refused"),
                expectedCode = UUHttpErrorCode.CONNECTION_FAILED // <-- adjust if needed
            )
        )
    }*/

    /**
     * Negative-path tests for UUHttpSession when the underlying transport throws.
     *
     * Assumptions:
     * - UUHttpSession calls a transport to execute the request (suspending or blocking).
     * - Transport exceptions are mapped to UUHttpError with a UUHttpErrorCode.
     * - Connectivity check passes (we’re testing transport-layer failures here).
     */
    @Nested
    inner class NegativeTests
    {
        @OptIn(ExperimentalAtomicApi::class)
        @Test
        fun `checkConnection fails with error`() = runBlocking {
            val injectedError = UUError(-5757, "UnitTestErrorDomain")
            val connectivity = mock<UUConnectivityProvider> {
                on { checkConnection() } doReturn injectedError
            }

            val session = UUHttpSession()

            val req = UUHttpRequest(url = "https://api.example.com/test")
            req.connectivityProvider = connectivity

            val response = session.executeRequest(req)
            var err = UUAssert.unwrap(response.error)
            assertEquals(injectedError.code, err.code)
            assertEquals(injectedError.domain, err.domain)
            assertEquals(UUHttpRequest.State.CheckConnection, response.request.state)
        }

        /*
        @OptIn(ExperimentalAtomicApi::class)
        @Test
        fun `openConnection fails with error`()
        {
            val injectedError = UUError(-5757, "UnitTestErrorDomain")
            val session = UUHttpSession()

            val req = UUHttpRequest(url = "https://api.example.com/test")

            var responseContainer = AtomicReference<UUHttpResponse?>(null)

            val latch = CountDownLatch(1)

            val url = req.toURL

            mockStatic(URL::class.java).use { mocked ->
                mocked.`when`<Any?> { url.openConnection() }
                    .thenThrow(RuntimeException("Mocked connection failure"))

                //assertThrows<RuntimeException> {
                  //  url.uuOpenConnection()
                //}
            }

            session.executeRequest(req)
            { response ->

                responseContainer.store(response)
                latch.countDown()
            }

            latch.await(5, TimeUnit.SECONDS)

            val response = UUAssert.unwrap(responseContainer.load())
            var err = UUAssert.unwrap(response.error)
            assertEquals(injectedError.code, err.code)
            assertEquals(injectedError.domain, err.domain)
            assertEquals(UUHttpRequest.State.OpenConnection, response.request.state)
        }*/

        /*
        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("exceptionCases")
        fun `maps transport exceptions to UUHttpError`(case: Case) = runBlocking {
            // Arrange
            val connectivity = mock<UUConnectivityProvider> {
                on { checkConnection() } doReturn null // connectivity OK; we're testing transport failures
            }

            // Mock transport to throw the case exception when invoked
            val transport = mock<UUHttpTransport> {
                onBlocking { execute(any()) } doAnswer { throw case.thrown }
            }

            val session = UUHttpSession(
                connectivityProvider = connectivity,
                transport = transport
            )

            val req = UUHttpRequest(url = "https://api.example.com/test")

            // Act
            val resp = session.execute(req)

            // Assert
            val err = resp.httpError
            assertNotNull(err, "Expected an error on response")
            assertEquals(case.expectedCode, err.code, "Unexpected UUHttpErrorCode")
            assertNull(resp.httpResponse, "Should not have an HTTP response on transport exception")
            assertTrue(resp.rawResponse == null || resp.rawResponse?.isEmpty() == true, "No raw body expected")

            // Transport should have been called once
            verify(transport, times(1)).execute(any())
            verifyNoMoreInteractions(transport)
        }

        @Test
        @DisplayName("Transport throws unknown exception -> maps to UNKNOWN / GENERIC_NETWORK_ERROR")
        fun mapsUnknownExceptionToGeneric() = runBlocking {
            val connectivity = mock<UUConnectivityProvider> {
                on { checkConnection() } doReturn null
            }
            val transport = mock<UUHttpTransport> {
                onBlocking { execute(any()) } doAnswer { throw IllegalStateException("boom") }
            }

            val session = UUHttpSession(connectivity, transport)
            val req = UUHttpRequest(url = "https://api.example.com/test")

            val resp = session.execute(req)

            val err = resp.httpError
            assertNotNull(err, "Expected an error on response")
            // Pick whichever generic code you use in your lib:
            // UNKNOWN, GENERIC_NETWORK_ERROR, UNEXPECTED_EXCEPTION, etc.
            assertEquals(UUHttpErrorCode.UNKNOWN, err.code, "Unexpected UUHttpErrorCode for unknown exception")
        }*/
    }

}