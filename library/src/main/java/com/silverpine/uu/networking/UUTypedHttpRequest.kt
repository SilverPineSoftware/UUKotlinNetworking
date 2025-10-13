package com.silverpine.uu.networking

open class UUTypedHttpRequest<SuccessType: Any, ErrorType: Any>(uri: UUHttpUri,
                                                                successClass: Class<SuccessType>,
                                                                errorClass: Class<ErrorType>,
):  UUHttpRequest(uri)
{
    init
    {
        responseHandler = UUTypedResponseHandler<SuccessType, ErrorType>(successClass, errorClass)
    }
}
