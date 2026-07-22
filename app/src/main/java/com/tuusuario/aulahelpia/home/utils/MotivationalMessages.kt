// MotivationalMessages.kt - VERSIÓN COMPLETA
package com.tuusuario.aulahelpia.home.utils

import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object MotivationalMessages {

    // ==================== MENSAJES ====================
    val calendarMessages = listOf(
        "📅 Planifica tu semana, domina tu mes",
        "🗓️ Un buen plan hoy, es un mañana exitoso",
        "⏰ Cada hora planificada es una hora ganada",
        "📆 La organización es la clave de la productividad",
        "🎯 Define tu semana, alcanza tus metas"
    )

    val dashboardMessages = listOf(
        "📊 Tu progreso diario construye tu éxito semanal",
        "🎯 Hoy es un buen día para superarte",
        "⚡ La consistencia es la clave del progreso",
        "💪 Cada tarea completada es una victoria",
        "🚀 Pequeños pasos, grandes resultados"
    )

    val profileMessages = listOf(
        "👤 Tu perfil, tu sistema, tu productividad",
        "🌟 Tú eres el arquitecto de tu tiempo",
        "🔧 Configura tu entorno para el éxito",
        "🎨 Planifica tu vida como una obra de arte",
        "✨ Organizando mi vida, un día a la vez"
    )

    val newTaskMessages = listOf(
        "🎯 Cada tarea planificada es un paso hacia tus metas",
        "📝 Un plan claro hoy, resultados mañana",
        "💡 ¿Qué quieres lograr? ¡Planifícalo ahora!",
        "✅ Define, planifica, ejecuta, revisa",
        "🚀 Esta nueva tarea te acerca a tus objetivos"
    )

    // ==================== MÉTODOS CON PERSISTENCIA ====================

    fun getCalendarMessage(context: Context): String {
        val prefs = context.getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
        var counter = prefs.getInt("calendar_counter", 0)

        val message = calendarMessages[counter % calendarMessages.size]

        counter++
        prefs.edit().putInt("calendar_counter", counter).apply()

        return message
    }

    fun getDashboardMessage(context: Context): String {
        val prefs = context.getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
        var counter = prefs.getInt("dashboard_counter", 0)

        val message = dashboardMessages[counter % dashboardMessages.size]

        counter++
        prefs.edit().putInt("dashboard_counter", counter).apply()

        return message
    }

    fun getProfileMessage(context: Context): String {
        val prefs = context.getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
        var counter = prefs.getInt("profile_counter", 0)

        val message = profileMessages[counter % profileMessages.size]

        counter++
        prefs.edit().putInt("profile_counter", counter).apply()

        return message
    }

    fun getNewTaskMessage(context: Context): String {
        val prefs = context.getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
        var counter = prefs.getInt("newtask_counter", 0)

        val message = newTaskMessages[counter % newTaskMessages.size]

        counter++
        prefs.edit().putInt("newtask_counter", counter).apply()

        return message
    }

    // ==================== ANIMACIONES ====================

    object Animations {

        // Efecto de fade in suave
        fun fadeIn(view: View, duration: Long = 500L) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Efecto de slide desde arriba
        fun slideIn(view: View, duration: Long = 400L) {
            view.translationY = -50f
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Efecto de scale (crece desde el centro)
        fun scaleIn(view: View, duration: Long = 450L) {
            view.scaleX = 0.8f
            view.scaleY = 0.8f
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(OvershootInterpolator(0.5f))
                .start()
        }

        // Efecto combinado (fade + slide suave)
        fun fadeSlide(view: View, duration: Long = 600L) {
            view.translationY = 20f
            view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Efecto de "pulse" sutil después de aparecer
        fun pulse(view: View) {
            view.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(200)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }

        // Obtener animación aleatoria (para variedad)
        fun applyRandom(view: View, counter: Int) {
            when (counter % 4) {
                0 -> fadeIn(view)
                1 -> slideIn(view)
                2 -> scaleIn(view)
                3 -> fadeSlide(view)
                else -> fadeIn(view)
            }
        }
    }

    // ==================== DEBUG ====================
    fun getCurrentCounters(context: Context): String {
        val prefs = context.getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
        return """
            🔍 CONTADORES:
            • Calendario: ${prefs.getInt("calendar_counter", 0)}
            • Dashboard: ${prefs.getInt("dashboard_counter", 0)}  
            • Perfil: ${prefs.getInt("profile_counter", 0)}
            • Nueva Tarea: ${prefs.getInt("newtask_counter", 0)}
        """.trimIndent()
    }
}