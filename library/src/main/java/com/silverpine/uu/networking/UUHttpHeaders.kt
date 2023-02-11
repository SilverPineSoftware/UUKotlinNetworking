package com.silverpine.uu.networking

import com.silverpine.uu.logging.UULog

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

    fun log(method: String, tag: String)
    {
        forEach()
        { key: String?, values: List<String>? ->
            UULog.d(javaClass, method, "$tag: $key=${values?.joinToString(",")}")
        }
    }
}