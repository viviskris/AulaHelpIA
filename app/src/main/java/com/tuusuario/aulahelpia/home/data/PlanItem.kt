package com.tuusuario.aulahelpia.home.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.tuusuario.aulahelpia.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// 🎯 ENUM DE CATEGORÍAS CON COLORES
enum class TaskCategory(val colorName: String, val displayName: String) {
    SALUD("salud", "Salud & Bienestar"),
    PERSONAL("personal", "Vida Personal"),
    TRABAJO("trabajo", "Trabajo & Proyectos"),
    APRENDIZAJE("aprendizaje", "Aprendizaje"),
    PRIORIDAD("prioridad", "Prioridades Altas"),
}

// 🆕 ENUM DE ESTADOS PARA TAREAS/EVENTOS
enum class TaskState(val displayName: String, val colorRes: Int) {
    ACTIVO("Activo", R.color.primary_pastel),           // 🔵 Azul
    COMPLETADO("Completado", R.color.completed_green),  // 🟢 Verde
    VENCIDO("Vencido", R.color.overdue_red),            // 🔴 Rojo
    REPROGRAMADO("REPROGRAMADO", R.color.pending_orange) // 🟡 Naranja
}

@Entity(tableName = "plan_items")
@TypeConverters(ModuleTypeConverter::class)
data class PlanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val date: String,  // Formato: "yyyy-MM-dd"
    val time: String = "09:00",
    val moduleType: ModuleType = ModuleType.TASK,
    val priority: Int = 1,
    val isCompleted: Boolean = false,
    val duration: Int = 0,
    val category: String = TaskCategory.PERSONAL.name,
    val taskState: String = TaskState.ACTIVO.name,
    val notificationSoundUri: String = "",
    val notificationVibration: Boolean = true,
    val notificationLedColor: String? = null
) {
    // Formatter para fechas (compatible con minSdk 24)
    companion object {
        private val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    // 🎨 MÉTODO PARA OBTENER COLOR SEGÚN CATEGORÍA - CORREGIDO
    fun getCategoryColorRes(): Int {
        return when (TaskCategory.valueOf(category)) {
            TaskCategory.SALUD -> R.color.exercise_pastel
            TaskCategory.PERSONAL -> R.color.personal_pastel
            TaskCategory.TRABAJO -> R.color.task_pastel
            TaskCategory.APRENDIZAJE -> R.color.study_pastel
            TaskCategory.PRIORIDAD -> R.color.important_pastel
        }
    }

    // 🆕 MÉTODO PARA OBTENER COLOR SEGÚN ESTADO
    fun getStateColorRes(): Int {
        return when (TaskState.valueOf(taskState)) {
            TaskState.ACTIVO -> R.color.primary_pastel
            TaskState.COMPLETADO -> R.color.completed_green
            TaskState.VENCIDO -> R.color.overdue_red
            TaskState.REPROGRAMADO -> R.color.pending_orange
        }
    }

    // 🆕 MÉTODO PARA OBTENER NOMBRE LEGIBLE DE CATEGORÍA
    fun getCategoryDisplayName(): String {
        return TaskCategory.valueOf(category).displayName
    }

    // 🆕 MÉTODO PARA OBTENER NOMBRE LEGIBLE DE ESTADO
    fun getStateDisplayName(): String {
        return TaskState.valueOf(taskState).displayName
    }

    // 🆕 MÉTODO PARA VERIFICAR SI ESTÁ VENCIDO (compatible con minSdk 24)
    fun isOverdue(): Boolean {
        return try {
            val eventDate = isoFormatter.parse(date)
            val today = Calendar.getInstance()

            // Comparar fechas (sin hora)
            val eventCalendar = Calendar.getInstance().apply { time = eventDate }

            // Resetear horas para comparar solo fechas
            eventCalendar.set(Calendar.HOUR_OF_DAY, 0)
            eventCalendar.set(Calendar.MINUTE, 0)
            eventCalendar.set(Calendar.SECOND, 0)
            eventCalendar.set(Calendar.MILLISECOND, 0)

            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            // Verificar si la fecha del evento es anterior a hoy Y no está completado
            eventCalendar.before(today) && taskState != TaskState.COMPLETADO.name
        } catch (e: Exception) {
            false // Si hay error de parsing, no marcar como vencido
        }
    }

    // 🆕 MÉTODO PARA ACTUALIZAR ESTADO AUTOMÁTICAMENTE (compatible)
    fun updateStateAutomatically(): PlanItem {
        val newState = when {
            isCompleted -> TaskState.COMPLETADO
            isOverdue() -> TaskState.VENCIDO
            else -> TaskState.valueOf(taskState)
        }
        return this.copy(taskState = newState.name)
    }
}