package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import com.silverpine.uu.networking.parsers.UUTypedStreamParser

/**
 * [UUBaseResponseHandler] that deserializes JSON success and error bodies into distinct types.
 *
 * [successParser] decodes 2xx bodies as [SuccessType]; [errorParser] decodes non-success bodies as
 * [ErrorType]. Both use [UUTypedStreamParser] backed by [com.silverpine.uu.core.UUJson].
 *
 * ### Example
 * ```kotlin
 * request.responseHandler = UUTypedResponseHandler(UserDto::class.java, ApiError::class.java)
 * ```
 *
 * @param SuccessType type deserialized from successful responses.
 * @param ErrorType type deserialized from error responses (for example an API error DTO).
 * @param successClass runtime class for [SuccessType].
 * @param errorClass runtime class for [ErrorType].
 * @see UUTypedStreamParser
 * @see UUBaseResponseHandler
 */
open class UUTypedResponseHandler<SuccessType : Any, ErrorType : Any>(
    successClass: Class<SuccessType>,
    errorClass: Class<ErrorType>,
) : UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUTypedStreamParser(successClass)
    override val errorParser: UUHttpStreamParser = UUTypedStreamParser(errorClass)
}
