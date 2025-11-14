package com.tuusuario.creciendojuntos.home.model

/**
 * Data class que representa el progreso actual del embarazo
 * @param currentWeek Semana actual de embarazo (1-40)
 * @param daysInCurrentWeek Días en la semana actual (0-6)
 * @param weeksRemaining Semanas restantes para el parto
 * @param babySize Tamaño comparativo del bebé (ej: "como una frambuesa")
 * @param babyDevelopment Descripción del desarrollo del bebé esta semana
 * @param progressPercentage Porcentaje de progreso total (0-100)
 */
data class PregnancyProgress(
    val currentWeek: Int,
    val daysInCurrentWeek: Int,
    val weeksRemaining: Int,
    val babySize: String,
    val babyDevelopment: String,
    val progressPercentage: Float = 0f
)