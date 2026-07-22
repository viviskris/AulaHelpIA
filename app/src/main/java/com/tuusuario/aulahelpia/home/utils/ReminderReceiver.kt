package com.tuusuario.aulahelpia.home.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.tuusuario.aulahelpia.home.data.PlanItem
import com.tuusuario.aulahelpia.home.data.ModuleType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("🔔 REMINDERRECEIVER ACTIVADO!")

        // Obtener datos de la TAREA desde el Intent
        val title = intent.getStringExtra("task_title") ?: "Tarea"
        val description = intent.getStringExtra("task_description") ?: ""
        val taskTypeString = intent.getStringExtra("task_type") ?: "TASK"
        val reminderType = intent.getStringExtra("reminder_type") ?: "recordatorio"
        val taskTime = intent.getStringExtra("task_time") ?: ""

        println("📅 RECORDATORIO RECIBIDO:")
        println("   - Tarea: $title")
        println("   - Descripción: $description")
        println("   - Tipo de tarea: $taskTypeString")
        println("   - Tipo de recordatorio: $reminderType")
        println("   - Hora de la tarea: $taskTime")

        // Convertir string a ModuleType enum
        val taskType = try {
            ModuleType.valueOf(taskTypeString)
        } catch (e: IllegalArgumentException) {
            ModuleType.TASK // Valor por defecto
        }

        // CREAR TAREA TEMPORAL PARA LA NOTIFICACIÓN
        val task = PlanItem(
            id = System.currentTimeMillis(),
            title = title,
            description = description,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), // ✅ Compatible
            time = if (taskTime.isNotEmpty()) taskTime else "09:00",
            moduleType = taskType // ✅ Usando enum ModuleType
        )

        // MOSTRAR LA NOTIFICACIÓN DEL RECORDATORIO
        val notificationHelper = NotificationHelper(context)

        // Personalizar el mensaje según el tipo de recordatorio
        when (reminderType) {
            "30min" -> {
                notificationHelper.showTaskReminder(task, 30, "30min")
                println("⏰ Notificación de 30 minutos antes mostrada")
            }
            "1hora" -> {
                notificationHelper.showTaskReminder(task, 60, "1hora")
                println("⏰ Notificación de 1 hora antes mostrada")
            }
            "1dia" -> {
                notificationHelper.showTaskReminder(task, 24 * 60, "1dia")
                println("⏰ Notificación de 1 día antes mostrada")
            }
            else -> {
                notificationHelper.showTaskReminder(task, 0, "recordatorio")
                println("⏰ Notificación de recordatorio genérico mostrada")
            }
        }

        // OPCIONAL: Mostrar Toast para debugging
        Toast.makeText(
            context,
            "🔔 Recordatorio: $title",
            Toast.LENGTH_SHORT
        ).show()

        println("✅ RECORDATORIO PROCESADO EXITOSAMENTE")
    }
}