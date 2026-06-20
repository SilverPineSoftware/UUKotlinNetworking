package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class UURemoteApiTests
{
    private val requestUrl = "https://api.example.com/resource"

    private fun testRequest(): UUHttpRequest = UUHttpRequest(url = requestUrl)

    private fun successResponse(request: UUHttpRequest, body: Any? = "ok"): UUHttpResponse =
        UUHttpResponse(request = request, parsedResponse = body)

    /**
     * Auth-failure response for unit tests. Uses [UUError] directly (no [Bundle]) because JVM
     * unit tests do not provide Android framework stubs for [android.os.Bundle].
     */
    private fun authNeededResponse(request: UUHttpRequest): UUHttpResponse =
        UUHttpResponse(
            request = request,
            error = UUError(
                code = UUNetworkErrorCode.AUTHORIZATION_NEEDED.value,
                domain = UUNetworkError.DOMAIN,
            ),
        )

    /**
     * Records [renewApiAuthorization] and delegates HTTP to a mocked [UUHttpSession].
     */
    private open class TestRemoteApi(
        session: UUHttpSession,
    ) : UURemoteApi(session)
    {
        var apiAuthorizationNeeded: Boolean = false
        var renewResult: UURenewAuthorizationResponse =
            UURenewAuthorizationResponse(didAttempt = true, error = null)
        var renewDelayMs: Long = 50L
        var blockRenewalUntilReleased: Boolean = false

        val renewCallCount = AtomicInteger(0)

        private var renewStartedLatch = CountDownLatch(1)
        private var releaseRenewal = CountDownLatch(1)

        fun awaitRenewStarted(timeoutSeconds: Long = 5)
        {
            if (!renewStartedLatch.await(timeoutSeconds, TimeUnit.SECONDS))
            {
                throw AssertionError("renewApiAuthorization was not started in time")
            }
        }

        fun releaseBlockedRenewal()
        {
            releaseRenewal.countDown()
        }

        fun resetRenewalGate()
        {
            renewStartedLatch = CountDownLatch(1)
            releaseRenewal = CountDownLatch(1)
        }

        override suspend fun isApiAuthorizationNeeded(): Boolean = apiAuthorizationNeeded

        override suspend fun renewApiAuthorization(): UURenewAuthorizationResponse
        {
            renewCallCount.incrementAndGet()
            renewStartedLatch.countDown()
            if (blockRenewalUntilReleased)
            {
                releaseRenewal.await(5, TimeUnit.SECONDS)
            }
            delay(renewDelayMs)
            return renewResult
        }
    }

    private suspend fun stubSession(
        handler: suspend (UUHttpRequest) -> UUHttpResponse,
    ): UUHttpSession
    {
        val session = mock<UUHttpSession>()
        whenever(session.execute(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<UUHttpRequest>(0)
            runBlocking { handler(request) }
        }
        return session
    }

    @Nested
    inner class ProactiveRenewal
    {
        @Test
        fun skipsRenewalWhenAuthorizationIsNotNeeded() = runBlocking {
            val request = testRequest()
            val executeCount = AtomicInteger(0)
            val session = stubSession { req ->
                executeCount.incrementAndGet()
                successResponse(req)
            }
            val api = TestRemoteApi(session)

            val response = api.execute(request)

            assertEquals("ok", response.parsedResponse)
            assertEquals(0, api.renewCallCount.get())
            assertEquals(1, executeCount.get())
        }

        @Test
        fun returnsRenewalErrorWithoutExecutingRequest() = runBlocking {
            val renewalError = UUError(-42, "RenewalTest")
            val request = testRequest()
            val executeCount = AtomicInteger(0)
            val session = stubSession { req ->
                executeCount.incrementAndGet()
                successResponse(req)
            }
            val api = TestRemoteApi(session).apply {
                apiAuthorizationNeeded = true
                renewResult = UURenewAuthorizationResponse(didAttempt = false, renewalError)
            }

            val response = api.execute(request)

            assertSame(renewalError, response.error)
            assertEquals(1, api.renewCallCount.get())
            assertEquals(0, executeCount.get())
        }

        @Test
        fun coalescesConcurrentProactiveRenewalsIntoOneCall() = runBlocking {
            val request = testRequest()
            val executeCount = AtomicInteger(0)
            val session = stubSession { req ->
                executeCount.incrementAndGet()
                successResponse(req)
            }
            val api = TestRemoteApi(session).apply {
                apiAuthorizationNeeded = true
                renewDelayMs = 150L
            }

            coroutineScope {
                val responses = List(8) {
                    async { api.execute(request) }
                }.awaitAll()

                responses.forEach { assertEquals("ok", it.parsedResponse) }
            }

            assertEquals(1, api.renewCallCount.get())
            assertEquals(8, executeCount.get())
        }
    }

    @Nested
    inner class ReactiveRenewal
    {
        @Test
        fun retriesOnceAfterAuthorizationNeededErrorAndSuccessfulRenewal() = runBlocking {
            val request = testRequest()
            val executeCount = AtomicInteger(0)
            val session = stubSession { req ->
                if (executeCount.getAndIncrement() == 0)
                {
                    authNeededResponse(req)
                }
                else
                {
                    successResponse(req, "after-renew")
                }
            }
            val api = TestRemoteApi(session)

            val response = api.execute(request)

            assertEquals("after-renew", response.parsedResponse)
            assertNull(response.error)
            assertEquals(1, api.renewCallCount.get())
            assertEquals(2, executeCount.get())
        }

        @Test
        fun returnsRenewalErrorInsteadOfOriginalAuthErrorWhenReactiveRenewFails() = runBlocking {
            val request = testRequest()
            val renewalError = UUError(-99, "ReactiveRenewFailed")
            val executeCount = AtomicInteger(0)
            val session = stubSession { req ->
                executeCount.incrementAndGet()
                authNeededResponse(req)
            }
            val api = TestRemoteApi(session).apply {
                renewResult = UURenewAuthorizationResponse(didAttempt = true, renewalError)
            }

            val response = api.execute(request)

            assertSame(renewalError, response.error)
            assertEquals(1, api.renewCallCount.get())
            assertEquals(1, executeCount.get())
        }

        @Test
        fun doesNotRetryRequestWhenRenewalReturnsDidAttemptFalse() = runBlocking {
            val request = testRequest()
            val executeCount = AtomicInteger(0)
            val session = stubSession { req ->
                executeCount.incrementAndGet()
                authNeededResponse(req)
            }
            val api = TestRemoteApi(session).apply {
                renewResult = UURenewAuthorizationResponse(didAttempt = false, error = null)
            }

            val response = api.execute(request)

            assertNotNull(response.error)
            assertEquals(UUNetworkErrorCode.AUTHORIZATION_NEEDED, response.error?.uuNetworkErrorCode())
            assertEquals(1, api.renewCallCount.get())
            assertEquals(1, executeCount.get())
        }

        @Test
        fun coalescesConcurrentReactiveRenewalsAfterAuthErrors() = runBlocking {
            val request = testRequest()
            val session = stubSession { req -> authNeededResponse(req) }
            val api = TestRemoteApi(session).apply {
                renewDelayMs = 150L
            }

            coroutineScope {
                List(6) { async { api.execute(request) } }.awaitAll()
            }

            assertEquals(1, api.renewCallCount.get())
        }
    }

    @Nested
    inner class RenewalSequencing
    {
        @Test
        fun startsNewRenewalAfterPreviousInFlightRenewalCompletes() = runBlocking {
            val request = testRequest()
            val session = stubSession { req -> successResponse(req) }
            val api = TestRemoteApi(session).apply {
                apiAuthorizationNeeded = true
            }

            api.execute(request)
            assertEquals(1, api.renewCallCount.get())

            api.resetRenewalGate()
            api.execute(request)
            assertEquals(2, api.renewCallCount.get())
        }

        @Test
        fun lateWaiterJoinsInFlightRenewalInsteadOfStartingAnother() = runBlocking {
            val request = testRequest()
            val session = stubSession { req -> successResponse(req) }
            val api = TestRemoteApi(session).apply {
                apiAuthorizationNeeded = true
                blockRenewalUntilReleased = true
            }

            val first = async(Dispatchers.Default) { api.execute(request) }
            yield()
            api.awaitRenewStarted()
            val second = async(Dispatchers.Default) { api.execute(request) }

            api.releaseBlockedRenewal()
            first.await()
            second.await()

            assertEquals(1, api.renewCallCount.get())
        }
    }
}
