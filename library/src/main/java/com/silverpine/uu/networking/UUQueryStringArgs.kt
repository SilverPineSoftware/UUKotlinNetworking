package com.silverpine.uu.networking

import android.net.Uri

class UUQueryStringArgs: HashMap<String, String>()
{
    override fun toString(): String
    {
        val builder = Uri.Builder()

        forEach()
        { key, value ->
            builder.appendQueryParameter(key, value)
        }

        return builder.build().toString()
    }
}