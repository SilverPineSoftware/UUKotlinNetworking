package com.silverpine.uu.networking

import java.net.URL

class UUHttpUri(
    val url: String,
    val query: UUQueryStringArgs = UUQueryStringArgs(),
    val path: UUPathArgs = UUPathArgs())
{
    fun toURL(): URL?
    {
        return try
        {
            URL(toString())
        }
        catch (ex: Exception)
        {
            null
        }
    }

    override fun toString(): String
    {
        return "${url}${path}${query}"
    }
}