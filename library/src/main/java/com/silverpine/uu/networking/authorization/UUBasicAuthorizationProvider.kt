package com.silverpine.uu.networking.authorization

import com.silverpine.uu.core.uuAsciiByteArray
import com.silverpine.uu.core.uuBase64
import com.silverpine.uu.networking.UUHttpRequest
import com.silverpine.uu.networking.UURemoteApi

/**
 * [HTTP Basic authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Authentication#basic_authentication_scheme)
 * for the `Authorization` header.
 *
 * Credentials are formed as `username:password`, encoded with ASCII bytes and Base64, then sent as:
 *
 * ```
 * Authorization: Basic <base64-credentials>
 * ```
 *
 * This matches REST APIs that accept a username and password on every request, or an API key as
 * [userName] with a shared [password].
 *
 * [formatAuthorization] returns `null` (and [attachAuthorization] adds no header) when:
 * - [userName] or [password] is `null`
 * - either value is empty
 * - ASCII or Base64 encoding fails
 *
 * [userName] and [password] may be updated at any time; the next request uses the current values.
 *
 * Example (default provider on a remote API):
 * ```
 * api.defaultAuthorizationProvider = UUBasicAuthorizationProvider(
 *     userName = "my-api-key",
 *     password = "my-secret",
 * )
 * ```
 *
 * Example (single request):
 * ```
 * request.authorizationProvider = UUBasicAuthorizationProvider(userName, password)
 * ```
 *
 * @param userName User name or API key identifier.
 * @param password Password or secret paired with [userName].
 *
 * See also [RFC 7617](https://datatracker.ietf.org/doc/html/rfc7617) (HTTP Basic Access Authentication).
 *
 * @see UUHttpAuthorizationProvider
 * @see UURemoteApi.defaultAuthorizationProvider
 * @see UUHttpRequest.authorizationProvider
 */
open class UUBasicAuthorizationProvider(
    var userName: String?,
    var password: String?,
) : UUHttpAuthorizationProvider("Basic", null)
{
    /**
     * Returns Base64-encoded `userName:password`, or `null` if credentials are missing or cannot
     * be encoded.
     *
     * @return Base64 credential payload for the `Basic` scheme, or `null` to skip the header.
     */
    override fun formatAuthorization(): String?
    {
        val user = userName ?: return null
        val pwd = password ?: return null

        if (user.isNotEmpty() && pwd.isNotEmpty())
        {
            val authorizationData = "$user:$pwd".uuAsciiByteArray() ?: return null
            return authorizationData.uuBase64().getOrNull()
        }

        return null
    }
}
