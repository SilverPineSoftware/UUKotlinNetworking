package com.silverpine.uu.networking

interface UUHttpDataParser
{
    fun parse(data: ByteArray): Any?
}