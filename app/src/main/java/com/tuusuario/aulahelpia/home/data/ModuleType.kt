package com.tuusuario.aulahelpia.home.data

enum class ModuleType(
    val displayName: String,
    val emoji: String
) {
    TASK("💼 Trabajo & Proyectos", "💼"),
    EXERCISE("💪 Salud & Bienestar", "💪"),
    STUDY("📚 Aprendizaje", "📚"),
    NUTRITION("🍎 Nutrición", "🍎"),
    PERSONAL("⭐ Vida Personal", "⭐"),
    IMPORTANT("🚀 Prioridades Altas", "🚀");

    companion object {
        fun getDisplayNames(): Array<String> {
            return values().map { it.displayName }.toTypedArray()
        }

        fun fromDisplayName(displayName: String): ModuleType {
            return values().find { it.displayName == displayName } ?: TASK
        }
    }
}