package com.tuusuario.aulahelpia.home.utils

import android.content.Context

object GeminiApiKey {
    fun getApiKey(context: Context): String {
        val properties = java.util.Properties()
        try {
            val inputStream = context.assets.open("local.properties")
            properties.load(inputStream)
            inputStream.close()
            return properties.getProperty("GEMINI_API_KEY") ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}