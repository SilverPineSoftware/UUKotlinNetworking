# UUKotlinNetworking

Coroutine-based HTTP client utilities for Silverpine UU Android apps: requests, sessions, typed APIs, connectivity checks, and structured network errors.

## Maven coordinates

| Artifact | Coordinates |
|----------|-------------|
| Networking | `com.silverpine.uu:uu-networking-ktx` |

Published to [Maven Central](https://central.sonatype.com/search?q=com.silverpine.uu) under the `com.silverpine.uu` group.

## What's included

### HTTP stack

- **`UUHttpRequest`** / **`UUHttpResponse`** — URL building (paths, query), headers, body, timeouts, TLS options.
- **`UUHttpSession`** — `executeRequest` pipeline with explicit request states and error propagation.
- **`UUHttpBody`**, **`UUJsonBody`** — request payload encoding with `UUNetworkError` on failure.

### Errors

- **`UUNetworkError`** / **`UUNetworkErrorCode`** — typed codes with descriptions, resolutions, and `Bundle` user info (status code, URL, response body).
- Exception mapping (timeouts, unknown host, SSL/socket failures).

### Response handling

- **`UUHttpResponseHandler`**, **`UUBaseResponseHandler`** — gzip/deflate, success vs. error stream selection.
- **Parsers** — binary, text, typed JSON (`UUTypedStreamParser`), file download (`UUDownloadFileStreamParser`).
- **`uuHttpStreamParser { }`** factory for custom suspending parsers (avoid raw SAM on suspend interfaces).

### Higher-level APIs

- **`UURemoteApi`** — authorized request execution with coalesced token renewal (proactive and reactive).
- **`UUTypedHttpRequest`** / **`UUTypedHttpResponse`** — generic success/error types.
- **`UURemoteImage`** / **`UURemoteImageDownloader`** — image fetch helpers.
- **Authorization** — `UUHttpAuthorizationProvider`, `UUBasicAuthorizationProvider`.
- **Connectivity** — `UUConnectivity`, `UUNetworkConnectivityProvider` (captive portal / no internet detection).

## Gradle dependency

```kotlin
dependencies {
    implementation("com.silverpine.uu:uu-networking-ktx:<version>")
    implementation("com.silverpine.uu:uu-core-ktx:<version>")
}
```

## Requirements

- `UUConnectivity.init(...)` in `Application.onCreate` when using default connectivity checks
- JUnit 5 + Robolectric recommended for JVM unit tests of error paths (`Bundle` support)
- UU Kotlin build catalog (`uu_build`) and GitHub Packages access for `UUKotlinBuild`

## Changes in this release

- Exhaustive **`UUNetworkErrorCode`** descriptions and resolutions wired through `UUNetworkError.makeError`.
- **`UURemoteApi`** authorization renewal coalescing for concurrent requests.
- Response parser pipeline runs on `Dispatchers.IO`; suspending parser factory documented.
- Extensive JUnit 5 / Robolectric unit tests for session, parsers, handlers, and error mapping.
- Dokka Javadoc JAR published alongside the AAR.

---

For prior versions and snapshots, see [GitHub Releases](https://github.com/SilverpineSoftware/UUKotlinNetworking/releases).
