package com.silverpine.uu.networking

/**
 * Enumeration of common HTTP headers used in API and web service communication.
 *
 * This enum provides type-safe access to the most frequently used HTTP headers
 * when constructing or parsing requests and responses in `UUKotlinNetworking`.
 * Each enum constant maps to its canonical header name as defined by
 * [RFC 9110 - HTTP Semantics](https://datatracker.ietf.org/doc/html/rfc9110)
 * and related HTTP specifications.
 *
 * ### Usage
 * ```kotlin
 * val request = UUHttpRequest("https://api.example.com/data")
 * request.setHeader(UUHttpHeader.ContentType, "application/json")
 * request.setHeader(UUHttpHeader.Authorization, "Bearer $token")
 * ```
 *
 * ### Header categories
 * - **Core request headers:** `Accept`, `Content-Type`, `Authorization`, `User-Agent`
 * - **Caching headers:** `ETag`, `If-None-Match`, `Cache-Control`
 * - **Response metadata:** `Location`, `Retry-After`, `Server`
 * - **Custom headers:** `X-Request-Id`, `X-Api-Key`
 *
 * This list focuses on the most common headers relevant to REST-ful web APIs
 * and omits browser-specific or proxy-related headers for simplicity.
 *
 * @since 1.0.0
 * @property key The canonical string name of the HTTP header (e.g. `"Content-Type"`).
 */
enum class UUHttpHeader(val key: String)
{
    // --- Core request headers ---
    Accept("Accept"),
    AcceptEncoding("Accept-Encoding"),
    AcceptLanguage("Accept-Language"),
    Authorization("Authorization"),
    CacheControl("Cache-Control"),
    Connection("Connection"),
    ContentType("Content-Type"),
    ContentLength("Content-Length"),
    ContentEncoding("Content-Encoding"),
    Host("Host"),
    UserAgent("User-Agent"),

    // --- Conditional / caching ---
    ETag("ETag"),
    IfNoneMatch("If-None-Match"),
    IfModifiedSince("If-Modified-Since"),
    LastModified("Last-Modified"),
    Expires("Expires"),

    // --- Response metadata ---
    Location("Location"),
    Server("Server"),
    RetryAfter("Retry-After"),

    // --- Security and transport ---
    StrictTransportSecurity("Strict-Transport-Security"),

    // --- Custom / app-level ---
    RequestId("X-Request-Id"),
    ApiKey("X-Api-Key");

    override fun toString(): String = key
}