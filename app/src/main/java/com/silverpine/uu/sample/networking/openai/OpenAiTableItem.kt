package com.silverpine.uu.sample.networking.openai

data class OpenAiTableItem(
    val id: Int,
    val prompt: String,
    val answer: String)
{
    val timestamp: Long = System.currentTimeMillis()
}