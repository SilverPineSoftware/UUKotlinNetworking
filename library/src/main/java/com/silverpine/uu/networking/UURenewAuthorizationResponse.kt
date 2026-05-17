package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError

data class UURenewAuthorizationResponse(
    val didAttempt: Boolean,
    val error: UUError?
)