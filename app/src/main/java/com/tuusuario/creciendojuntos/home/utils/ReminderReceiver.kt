package com.tuusuario.creciendojuntos.home.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel
import java.time.LocalDate
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("🔔 REMINDERRECEIVER ACTIVADO!")

        // Obtener datos del evento desde el Intent
        val title = intent.getStringExtra("event_title") ?: "Evento"
        val description = intent.getStringExtra("event_description") ?: ""
        val eventType = intent.getStringExtra("event_type") ?: "PERSONAL"
        val reminderType = intent.getStringExtra("reminder_type") ?: "recordatorio"
        val eventTime = intent.getStringExtra("event_time") ?: ""

        println("📅 RECORDATORIO RECIBIDO:")
        println("   - Evento: $title")
        println("   - Descripción: $description")
        println("   - Tipo de evento: $eventType")
        println("   - Tipo de recordatorio: $reminderType")
        println("   - Hora del evento: $eventTime")

        // 🆕 CREAR EVENTO TEMPORAL PARA LA NOTIFICACIÓN
        val event = PregnancyViewModel.CalendarEvent(
            id = System.currentTimeMillis(),
            title = title,
            description = description,
            date = LocalDate.now(), // Fecha no crítica para la notificación
            time = if (eventTime.isNotEmpty()) LocalTime.parse(eventTime) else LocalTime.now(),
            type = eventType
        )

        // 🆕 MOSTRAR LA NOTIFICACIÓN DEL RECORDATORIO
        val notificationHelper = NotificationHelper(context)

        // Personalizar el mensaje según el tipo de recordatorio
        when (reminderType) {
            "30min" -> {
                notificationHelper.showEventReminder(event, 30, "30min")
                println("⏰ Notificación de 30 minutos antes mostrada")
            }
            "1hora" -> {
                notificationHelper.showEventReminder(event, 60, "1hora")
                println("⏰ Notificación de 1 hora antes mostrada")
            }
            "1dia" -> {
                notificationHelper.showEventReminder(event, 24 * 60, "1dia")
                println("⏰ Notificación de 1 día antes mostrada")
            }
            else -> {
                notificationHelper.showEventReminder(event, 0, "recordatorio")
                println("⏰ Notificación de recordatorio genérico mostrada")
            }
        }

        // 🆕 OPCIONAL: Mostrar Toast para debugging (quitar en producción)
        Toast.makeText(
            context,
            "🔔 Recordatorio: $title",
            Toast.LENGTH_SHORT
        ).show()

        println("✅ RECORDATORIO PROCESADO EXITOSAMENTE")
    }
}