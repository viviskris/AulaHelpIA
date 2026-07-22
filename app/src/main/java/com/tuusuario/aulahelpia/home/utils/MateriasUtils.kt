package com.tuusuario.aulahelpia.home.utils

import android.content.Context
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.home.data.ModuleType

object MateriasUtils {

    // 🎯 MATERIAS FIJAS (4 principales) con emojis y colores
    val materiasFijas = listOf(
        "Matemáticas" to "📐",
        "Lengua Castellana" to "📖",
        "Biología" to "🧬",
        "Sociales" to "🌍"
    )

    // 🎨 PALETA DE COLORES PARA MATERIAS ADICIONALES (igual en perfil y calendario)
    private val coloresAdicionales = listOf(
        R.color.purple_neon,
        R.color.cyan_bright,
        R.color.important_pastel,
        R.color.personal_pastel,
        R.color.exercise_pastel,
        R.color.study_pastel
    )

    // 🎨 MAPA DE EMOJIS PARA MATERIAS ADICIONALES
    val emojiMap = mapOf(
        "Geometría" to "📐",
        "Estadística" to "📊",
        "Geografía" to "🌍",
        "Filosofía" to "🧠",
        "Teología" to "⛪",
        "Inglés" to "🇬🇧",
        "Química" to "🧪",
        "Física" to "⚡",
        "Educación Física" to "🏃",
        "Artes" to "🎨",
        "Ética" to "⚖️",
        "Religión" to "🕊️",
        "Tecnología" to "💻",
        "Economía" to "💰",
        "Política" to "🗳️",
        "Psicología" to "🧠"
    )

    // 🎨 MAPA DE COLORES PARA MATERIAS ADICIONALES
    val colorMap = mapOf(
        "Geometría" to R.color.study_pastel,
        "Estadística" to R.color.cyan_bright,
        "Geografía" to R.color.important_pastel,
        "Filosofía" to R.color.purple_neon,
        "Teología" to R.color.purple_neon,
        "Inglés" to R.color.exercise_pastel,
        "Química" to R.color.study_pastel,
        "Física" to R.color.study_pastel,
        "Educación Física" to R.color.exercise_pastel,
        "Artes" to R.color.personal_pastel,
        "Ética" to R.color.study_pastel,
        "Religión" to R.color.purple_neon,
        "Tecnología" to R.color.cyan_bright,
        "Economía" to R.color.exercise_pastel,
        "Política" to R.color.personal_pastel,
        "Psicología" to R.color.study_pastel
    )

    // 🎨 OBTENER COLOR PARA MATERIA FIJA
    fun getColorRes(materia: String): Int {
        return when (materia) {
            "Matemáticas" -> R.color.study_pastel
            "Lengua Castellana" -> R.color.personal_pastel
            "Biología" -> R.color.exercise_pastel
            "Sociales" -> R.color.important_pastel
            else -> R.color.purple_neon
        }
    }

    // 🎨 OBTENER COLOR PARA MATERIA (fija o adicional)
    fun getColorResForMateria(materia: String, context: Context, index: Int = 0): Int {
        // Si es fija, usar su color
        materiasFijas.forEach { (nombre, _) ->
            if (nombre == materia) {
                return getColorRes(nombre)
            }
        }
        // Buscar en el mapa de adicionales
        return colorMap[materia] ?: coloresAdicionales[index % coloresAdicionales.size]
    }

    // 🎨 OBTENER EMOJI PARA MATERIA (fija o adicional)
    fun getEmojiForMateria(materia: String): String {
        // Buscar en materias fijas
        materiasFijas.forEach { (nombre, emoji) ->
            if (nombre == materia) {
                return emoji
            }
        }
        // Buscar en el mapa de adicionales
        return emojiMap[materia] ?: "📚"
    }

    // 🎨 OBTENER COLOR PARA MATERIA ADICIONAL (basado en el nombre)
    fun getColorForAdicionalPorNombre(materia: String): Int {
        return colorMap[materia] ?: R.color.purple_neon
    }

    // 🎨 OBTENER COLOR PARA MATERIA ADICIONAL (usando la paleta)
    fun getColorForAdicional(index: Int): Int {
        return coloresAdicionales[index % coloresAdicionales.size]
    }

    // 📚 OBTENER TODAS LAS MATERIAS (fijas + adicionales)
    fun getMateriasGuardadas(context: Context): List<String> {
        val prefs = context.getSharedPreferences("aulahelpia_prefs", Context.MODE_PRIVATE)
        val materiasAdicionales = prefs.getStringSet("materias_adicionales", emptySet()) ?: emptySet()
        return materiasFijas.map { it.first } + materiasAdicionales
    }

    // 📚 OBTENER TODAS LAS MATERIAS (alias para getMateriasGuardadas)
    fun getMaterias(context: Context): List<String> {
        return getMateriasGuardadas(context)
    }

    // 📚 OBTENER ModuleType para una materia
    fun getModuleTypeForMateria(materia: String): ModuleType {
        return when {
            materia.contains("Matem", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Lengua", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Cienc", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Hist", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Ingl", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Físi", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Quím", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Biolog", ignoreCase = true) -> ModuleType.STUDY
            materia.contains("Educ", ignoreCase = true) -> ModuleType.PERSONAL
            materia.contains("Arte", ignoreCase = true) -> ModuleType.PERSONAL
            materia.contains("Músic", ignoreCase = true) -> ModuleType.PERSONAL
            materia.contains("Deport", ignoreCase = true) -> ModuleType.EXERCISE
            materia.contains("Educ Físi", ignoreCase = true) -> ModuleType.EXERCISE
            else -> ModuleType.TASK
        }
    }
}