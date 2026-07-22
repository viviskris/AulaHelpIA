package com.tuusuario.aulahelpia.home.utils

import android.content.Context
import com.tuusuario.aulahelpia.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class GeminiService(private val context: Context) {

    private val apiKey: String = context.getString(R.string.gemini_api_key)

    suspend fun generateResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = callGemini(prompt)
                response
            } catch (e: Exception) {
                "❌ Error: ${e.message}"
            }
        }
    }

    suspend fun generateResponseWithContext(prompt: String, contextInfo: String): String {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 GEMINI - Enviando pregunta: $prompt")
                println("🔍 GEMINI - Contexto: $contextInfo")

                val fullPrompt = """
                    Contexto del estudiante:
                    $contextInfo
                    
                    Pregunta del estudiante:
                    $prompt
                    
                    Responde de manera clara y útil, usando el contexto proporcionado.
                    Sé breve y directo.
                """.trimIndent()

                val response = callGemini(fullPrompt)
                response
            } catch (e: Exception) {
                println("❌ GEMINI - Error: ${e.message}")
                "❌ Error: ${e.message}"
            }
        }
    }
    private fun callGemini(prompt: String): String {
        // Lista de modelos gratuitos a probar
        val modelos = listOf(
            "gemini-1.5-flash",
            "gemini-1.5-flash-001",
            "gemini-1.0-pro",
            "gemini-pro",
            "gemini-1.0-pro-001"
        )

        for (modelo in modelos) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                println("🔍 GEMINI - Probando modelo: $modelo")
                println("🔍 GEMINI - URL: $url")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val jsonBody = """
    {
        "contents": [
            {
                "parts": [
                    {
                        "text": "$prompt"
                    }
                ]
            }
        ],
        "generationConfig": {
            "maxOutputTokens": 2048,
            "temperature": 0.7,
            "topP": 0.95
        }
    }
""".trimIndent()

                connection.outputStream.use { os ->
                    os.write(jsonBody.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                println("🔍 GEMINI - Modelo $modelo - Response Code: $responseCode")

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    println("✅ GEMINI - ¡Modelo $modelo funciona!")
                    return extractTextFromResponse(response)
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText() ?: "Error desconocido"
                    println("❌ GEMINI - Modelo $modelo - Error: $error")
                }
            } catch (e: Exception) {
                println("❌ GEMINI - Modelo $modelo - Excepción: ${e.message}")
            }
        }

        return "❌ Ningún modelo disponible. Verifica tu API Key."
    }

    private fun extractTextFromResponse(jsonResponse: String): String {
        try {
            println("🔍 GEMINI - JSON completo: $jsonResponse")

            // Buscar el texto en la respuesta JSON
            val pattern = "\"text\":\\s*\"([^\"]+)\"".toRegex()
            val match = pattern.find(jsonResponse)
            var text = match?.groupValues?.get(1)?.replace("\\n", "\n") ?: "No se pudo obtener respuesta"

            // Limpiar caracteres especiales
            text = text.replace("\\\"", "\"")
            text = text.replace("\\\\", "\\")
            text = text.replace("XError:", "").trim()

            return text
        } catch (e: Exception) {
            println("❌ GEMINI - Error extrayendo texto: ${e.message}")
            return "No se pudo procesar la respuesta"
        }
    }
}