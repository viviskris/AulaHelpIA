package com.tuusuario.creciendojuntos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val date: String, // Guardamos como String para simplificar
    val time: String = "09:00", // 🆕 AGREGAR HORA
    val type: String = "personal" // "medical", "personal", "milestone"
)