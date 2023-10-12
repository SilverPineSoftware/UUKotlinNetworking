package com.silverpine.uu.networking

import java.net.URL

data class UUHttpUri(
    var url: String,
    var query: UUQueryStringArgs = UUQueryStringArgs(),
    var path: UUPathArgs = UUPathArgs())
{
    internal val fullUrl: URL
        get() = URL(toString())

    override fun toString(): String
    {
        return "${url}${path}${query}"
    }
}