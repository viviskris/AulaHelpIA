package com.tuusuario.aulahelpia.home.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "horario")
data class HorarioItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dia: String,          // "Lunes", "Martes", ...
    val horaInicio: String,   // "07:00"
    val horaFin: String,      // "08:50"
    val materia: String,      // "Matemáticas"
    val profesor: String = "", // Opcional
    val aula: String = ""      // Opcional
)