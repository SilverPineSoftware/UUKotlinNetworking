package com.silverpine.uu.sample.networking.openai

import com.silverpine.uu.sample.networking.Prefs
import com.silverpine.uu.sample.networking.SecurePrefs

class OpenAiPrefsRepository(val prefs: Prefs = SecurePrefs())
{
    private val API_KEY = "api_key"
    private val MODEL_CHOICE = "model_choice"

    fun saveApiKey(key: String)
    {
        prefs.setString(API_KEY, key)
    }

    fun loadApiKey(): String
    {
        return prefs.getString(API_KEY) ?: ""
    }

    fun saveModelChoice(model: String)
    {
        prefs.setString(MODEL_CHOICE, model)
    }

    fun loadModelChoice(): String
    {
        return prefs.getString(MODEL_CHOICE) ?: ""
    }
}