/*package com.silverpine.uu.networking

import com.silverpine.uu.core.uuToJson
import com.silverpine.uu.core.uuUtf8ByteArray

open class UUHttpBody(var contentType: String, var content: ByteArray?)

inline fun <reified T: Any> uuJsonBody(content: T): UUHttpBody
{
    return UUHttpBody(UUContentType.APPLICATION_JSON, content.uuToJson()?.uuUtf8ByteArray())
}*/
