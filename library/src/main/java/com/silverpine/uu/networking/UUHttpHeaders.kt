package com.silverpine.uu.networking

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
}