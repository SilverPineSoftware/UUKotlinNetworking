package com.silverpine.uu.networking

class UUPathArgs(vararg arg: String): ArrayList<String>()
{
    override fun toString(): String
    {
        return joinToString("") { "/$it" }
    }

    init
    {
        arg.forEach()
        {
            add(it)
        }
    }
}