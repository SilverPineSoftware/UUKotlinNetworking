package com.silverpine.uu.networking.authorization

import com.silverpine.uu.networking.UUHttpHeaders

interface UUHttpAuthorizationProvider
{
    fun attachAuthorization(headers: UUHttpHeaders)
}