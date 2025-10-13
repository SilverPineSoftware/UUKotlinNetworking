package com.silverpine.uu.networking

import com.silverpine.uu.logging.UULog
import java.net.URLConnection

enum class UUHttpHeader(val key: String)
{
    AcceptEncoding("Accept-Encoding"),
    ContentType("Content-Type"),
    ContentLength("Content-Length"),
    ContentEncoding("Content-Encoding"),
    Authorization("Authorization")
}

class UUHttpHeaders(other: Map<String, List<String>> = mapOf()): HashMap<String, List<String>>(other)
{
    fun getSingle(key: String): String?
    {
        return this[key]?.firstOrNull()
    }

    fun putSingle(key: String, value: String)
    {
        this[key] = listOf(value)
    }

    fun put(header: UUHttpHeader, value: String)
    {
        putSingle(header.key, value)
    }

//    fun log(method: String, tag: String)
//    {
//        forEach()
//        { key: String?, values: List<String>? ->
//            UULog.d(javaClass, method, "$tag: $key=${values?.joinToString(",")}")
//        }
//    }
}