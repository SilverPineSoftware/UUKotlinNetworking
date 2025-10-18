package com.silverpine.uu.networking

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class UUHttpRequestTests
{
    @Test
    fun testConstructor()
    {
        val req = UUHttpRequest("")
        assertNotNull(req.id)
    }

}