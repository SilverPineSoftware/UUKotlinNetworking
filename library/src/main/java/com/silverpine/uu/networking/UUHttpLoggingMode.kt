package com.silverpine.uu.networking

enum class UUHttpLoggingMode
{
    Request,
    Response,
    RequestHeaders,
    ResponseHeaders,
    RequestBody,
    ResponseBody,
    Errors;

    companion object
    {
        val None: Array<UUHttpLoggingMode> = arrayOf()
        val Info: Array<UUHttpLoggingMode> = arrayOf(Request, Response)
        val Debug: Array<UUHttpLoggingMode> = arrayOf(Request, Response, RequestHeaders, ResponseHeaders)
        val Verbose: Array<UUHttpLoggingMode> = entries.toTypedArray()
    }
}