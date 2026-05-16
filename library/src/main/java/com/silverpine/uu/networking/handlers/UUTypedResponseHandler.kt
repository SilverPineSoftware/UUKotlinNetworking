package com.silverpine.uu.networking.handlers

import com.silverpine.uu.networking.parsers.UUHttpStreamParser
import com.silverpine.uu.networking.parsers.UUTypedStreamParser

open class UUTypedResponseHandler<SuccessType: Any, ErrorType: Any>(
    successClass: Class<SuccessType>,
    errorClass: Class<ErrorType>
): UUBaseResponseHandler()
{
    override val successParser: UUHttpStreamParser = UUTypedStreamParser(successClass)
    override val errorParser: UUHttpStreamParser = UUTypedStreamParser(errorClass)
}