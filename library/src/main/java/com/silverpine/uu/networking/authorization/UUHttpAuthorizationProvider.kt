package com.silverpine.uu.networking.authorization

import com.silverpine.uu.networking.UUHttpHeader
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UURemoteApi

/**
 * Supplies credentials for the HTTP [Authorization](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization)
 * request header.
 *
 * Subclasses define how credentials are formatted and which authentication [scheme] is used.
 * [UUHttpSession] invokes [attachAuthorization] immediately before the connection opens.
 *
 * The header is written as:
 *
 * ```
 * Authorization: <scheme> <credentials>
 * ```
 *
 * Examples: `Bearer eyJhbGciOiJIUzI1NiIs...`, `Basic dXNlcjpwYXNz`.
 *
 * **Per-request:** set [UUHttpRequest.authorizationProvider] on a single request.
 *
 * **Default for an API client:** set [UURemoteApi.defaultAuthorizationProvider]; [UURemoteApi.executeOneAuthorizedRequest]
 * copies it onto the request when none is set.
 *
 * If [formatAuthorization] returns `null` or an empty string, no `Authorization` header is added.
 *
 * Example (Bearer token):
 * ```
 * val request = UUHttpRequest("https://api.example.com/v1/items")
 * request.authorizationProvider = UUHttpAuthorizationProvider(
 *     scheme = "Bearer",
 *     authorization = accessToken,
 * )
 * ```
 *
 * Override [formatAuthorization] for dynamic credentials (see [UUBasicAuthorizationProvider]).
 *
 * @param scheme Authentication scheme, the first token in the header value (for example `"Bearer"` or
 *   `"Basic"`). Must not include a trailing space.
 * @param authorization Credential string used by the default [formatAuthorization] implementation.
 *   Subclasses may ignore this property and compute the value in [formatAuthorization] instead.
 *
 * @see UUBasicAuthorizationProvider
 * @see UUHttpRequest.authorizationProvider
 * @see UURemoteApi.defaultAuthorizationProvider
 * @see UUHttpSession
 */
open class UUHttpAuthorizationProvider(
    var scheme: String = "Bearer",
    var authorization: String?,
)
{
    /**
     * Returns the credential portion of the `Authorization` header (the text after [scheme]).
     *
     * The default implementation returns [authorization].
     *
     * @return Encoded credentials, or `null` to omit the header for this request. An empty string
     *   also omits the header.
     */
    open fun formatAuthorization(): String?
    {
        return authorization
    }

    /**
     * Adds `Authorization: <scheme> <credentials>` to [request] when [formatAuthorization] returns a
     * non-empty value.
     *
     * @param request The outbound request; [UUHttpRequest.headers] is updated in place.
     */
    open suspend fun attachAuthorization(request: UUHttpRequest)
    {
        val t = formatAuthorization() ?: return
        if (t.isNotEmpty())
        {
            request.headers.put(UUHttpHeader.Authorization, "$scheme $t")
        }
    }
}
