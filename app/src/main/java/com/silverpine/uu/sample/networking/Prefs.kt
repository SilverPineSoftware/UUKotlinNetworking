package com.silverpine.uu.sample.networking

import com.silverpine.uu.core.security.UUSecurePrefs

interface Prefs
{
    fun getString(key: String): String?
    fun setString(key: String, value: String?)
}

class SecurePrefs: Prefs
{
    override fun getString(key: String): String?
    {
        return UUSecurePrefs.getString(key)
    }

    override fun setString(key: String, value: String?)
    {
        UUSecurePrefs.setString(key, value)
    }
}

class PreviewPrefs(
    private val backing: MutableMap<String, String?> = mutableMapOf()
) : Prefs {

    override fun getString(key: String): String? = backing[key]

    override fun setString(key: String, value: String?)
    {
        if (value == null)
        {
            backing.remove(key)
        }
        else
        {
            backing[key] = value
        }
    }
}
