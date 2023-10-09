package com.silverpine.uu.networking.test

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
open class TestModel
{
    var id: String = ""
    var name: String = ""
    var level: Int = 0
    var xp: Int = 0
}