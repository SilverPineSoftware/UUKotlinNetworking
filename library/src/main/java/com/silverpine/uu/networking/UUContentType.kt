package com.silverpine.uu.networking

/**
 * Enumeration of common HTTP `Content-Type` values used in API, file upload, and media communication.
 *
 * This enum provides type-safe access to standard MIME media types
 * (as defined in [RFC 2046](https://datatracker.ietf.org/doc/html/rfc2046))
 * for use when setting or interpreting the `Content-Type` header
 * in `UUKotlinNetworking` requests and responses.
 *
 * The `Content-Type` header indicates the media type of the request or response body
 * and ensures both client and server correctly encode and decode payload data.
 *
 * ### Usage
 * ```kotlin
 * val request = UUHttpRequest("https://api.example.com/upload")
 * request.setHeader(UUHttpHeader.ContentType, UUContentType.AudioMpeg.value)
 * request.body = audioBytes
 * ```
 *
 * ### Categories
 * - **Structured data**
 *   - `application/json`
 *   - `text/plain`
 *   - `application/x-www-form-urlencoded`
 *   - `multipart/form-data`
 *   - `application/xml`
 * - **Binary / general-purpose**
 *   - `application/octet-stream`
 *   - `application/pdf`
 *   - `application/zip`
 *   - `text/csv`
 * - **Images**
 *   - `image/jpeg`
 *   - `image/png`
 *   - `image/gif`
 *   - `image/webp`
 *   - `image/svg+xml`
 * - **Audio**
 *   - `audio/mpeg`
 *   - `audio/wav`
 *   - `audio/ogg`
 *   - `audio/webm`
 *
 * @since 1.0.0
 * @property value The canonical MIME type string (e.g. `"application/json"`).
 *
 * @see UUHttpHeader.ContentType
 */
enum class UUContentType(val value: String)
{
    // --- Structured data ---
    /** JSON-encoded request or response body. */
    ApplicationJson("application/json"),

    /** Simple unformatted text payload. */
    TextPlain("text/plain"),

    /** Form URL-encoded data, e.g. `key=value&key2=value2`. */
    ApplicationFormUrlEncoded("application/x-www-form-urlencoded"),

    /** Multipart form data, commonly used for file uploads. */
    MultipartFormData("multipart/form-data"),

    /** XML-encoded data, used in SOAP or legacy APIs. */
    ApplicationXml("application/xml"),

    // --- Binary / file types ---
    /** Raw binary data stream, used for arbitrary file uploads or downloads. */
    ApplicationOctetStream("application/octet-stream"),

    /** PDF document. */
    ApplicationPdf("application/pdf"),

    /** ZIP archive file. */
    ApplicationZip("application/zip"),

    /** Comma-separated value (CSV) text. */
    TextCsv("text/csv"),

    // --- Image types ---
    /** JPEG image data (most common for photos). */
    ImageJpeg("image/jpeg"),

    /** PNG image data (lossless, supports transparency). */
    ImagePng("image/png"),

    /** GIF image data (supports animation). */
    ImageGif("image/gif"),

    /** WebP image data (modern compressed image format). */
    ImageWebp("image/webp"),

    /** SVG image data (vector graphics in XML). */
    ImageSvgXml("image/svg+xml"),

    // --- Audio types ---
    /** MPEG audio (MP3 format). */
    AudioMpeg("audio/mpeg"),

    /** WAV audio (uncompressed PCM). */
    AudioWav("audio/wav"),

    /** Ogg Vorbis audio container. */
    AudioOgg("audio/ogg"),

    /** WebM audio (Opus or Vorbis codec). */
    AudioWebm("audio/webm");

    override fun toString(): String = value

    companion object
    {
        /**
         * Attempts to map a raw MIME type string to a [UUContentType] value.
         *
         * @param value MIME type string to match (case-insensitive).
         * @return The corresponding enum constant, or `null` if no match is found.
         */
        fun fromValue(value: String?): UUContentType? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}