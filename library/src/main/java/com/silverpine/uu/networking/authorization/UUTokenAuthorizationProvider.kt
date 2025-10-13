package com.silverpine.uu.networking.authorization

open class UUTokenAuthorizationProvider(token: String?, scheme: String = "Bearer"):
    UUHttpAuthorizationProvider(scheme, token)