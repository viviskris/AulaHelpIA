package com.tuusuario.aulahelpia.home.data

data class HorarioStats(
    val totalClases: Int,
    val materiasFrecuentes: Map<String, Int>, // Materia -> Cantidad de clases
    val clasesCompletadas: Int,
    val progresoPorcentaje: Int
)