package com.silverpine.uu.sample.networking.shutterstock

import com.silverpine.uu.sample.networking.Prefs
import com.silverpine.uu.sample.networking.SecurePrefs

class ShutterstockPrefsRepository(val prefs: Prefs = SecurePrefs())
{
    private enum class Keys
    {
        UserName,
        Password
    }

    var userName: String
        get()
        {
            return prefs.getString(Keys.UserName.name) ?: ""
        }

        set(value)
        {
            prefs.setString(Keys.UserName.name, value)
        }

    var password: String
        get()
        {
            return prefs.getString(Keys.Password.name) ?: ""
        }

        set(value)
        {
            prefs.setString(Keys.Password.name, value)
        }
}