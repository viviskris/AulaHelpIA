package com.tuusuario.creciendojuntos.home.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.home.data.EventType
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_EVENTS = "event_reminders"
        const val CHANNEL_ID_MILESTONES = "milestone_reminders"
        const val NOTIFICATION_ID_EVENT = 1001

        // 🆕 CONSTANTES PARA RECORDATORIOS MÚLTIPLES
        const val REMINDER_30MIN = 30
        const val REMINDER_1HORA = 60
        const val REMINDER_1DIA = 24 * 60
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para recordatorios de eventos
            val eventChannel = NotificationChannel(
                CHANNEL_ID_EVENTS,
                "Recordatorios de Eventos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para ecografías, citas médicas e hitos personales"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // Canal para hitos del embarazo
            val milestoneChannel = NotificationChannel(
                CHANNEL_ID_MILESTONES,
                "Hitos del Embarazo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Actualizaciones semanales y hitos importantes del embarazo"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(eventChannel)
            notificationManager.createNotificationChannel(milestoneChannel)
        }
    }

    // 🆕 MÉTODO ACTUALIZADO PARA PROGRAMAR RECORDATORIOS MÚLTIPLES
    fun scheduleEventReminder(event: PregnancyViewModel.CalendarEvent, reminderMinutesBefore: Int = 60, reminderType: String = "default") {
        println("🔔 PROGRAMANDO RECORDATORIO: ${event.title}")
        println("   - Minutos antes: $reminderMinutesBefore")
        println("   - Tipo: $reminderType")

        // 🆕 PROGRAMAR CON ALARMMANAGER PARA EL FUTURO
        programarRecordatorioConAlarmManager(event, reminderMinutesBefore, reminderType)

        // ✅ MANTENER LA NOTIFICACIÓN INMEDIATA DE CONFIRMACIÓN (TU FUNCIONALIDAD ACTUAL)
        showEventReminder(event, reminderMinutesBefore, reminderType)
    }

    // 🆕 MÉTODO CORREGIDO PARA PROGRAMAR RECORDATORIOS CON ALARMMANAGER
    private fun programarRecordatorioConAlarmManager(event: PregnancyViewModel.CalendarEvent, minutesBefore: Int, reminderType: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Calcular la hora del recordatorio (hora del evento - minutos antes)
            val eventDateTime = LocalDateTime.of(event.date, event.time)
            val reminderDateTime = eventDateTime.minusMinutes(minutesBefore.toLong())

            // Convertir a milisegundos para AlarmManager
            val reminderTimeInMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Crear Intent único para cada recordatorio
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("event_title", event.title)
                putExtra("event_description", event.description)
                putExtra("event_type", event.type)
                putExtra("reminder_type", reminderType)
                putExtra("event_time", event.time.toString())
            }

            // 🆕 ID ÚNICO PARA CADA RECORDATORIO (evita sobreescritura)
            val uniqueId = (event.id.toString() + reminderType + minutesBefore).hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 🆕 ESTRATEGIA COMPATIBLE CON TODAS LAS VERSIONES DE ANDROID
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6.0+ - usar setExactAndAllowWhileIdle que requiere menos permisos
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeInMillis,
                        pendingIntent
                    )
                    println("✅ RECORDATORIO PROGRAMADO (setExactAndAllowWhileIdle)")
                }
                else -> {
                    // Android anterior a 6.0 - usar setExact normal
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeInMillis,
                        pendingIntent
                    )
                    println("✅ RECORDATORIO PROGRAMADO (setExact)")
                }
            }

            println("⏰ RECORDATORIO PROGRAMADO: ${event.title}")
            println("   - Hora del evento: ${event.time}")
            println("   - Recordatorio a las: $reminderDateTime")
            println("   - Minutos antes: $minutesBefore")
            println("   - ID único: $uniqueId")
            println("   - SDK Version: ${Build.VERSION.SDK_INT}")

        } catch (e: SecurityException) {
            println("⚠️ PERMISO DE ALARMA DENEGADO: ${e.message}")
            println("📱 Mostrando notificación inmediata como fallback...")

            // 🆕 FALLBACK: Mostrar notificación inmediata si no hay permisos
            showEventReminder(event, minutesBefore, "$reminderType (inmediato)")

        } catch (e: Exception) {
            println("❌ ERROR PROGRAMANDO RECORDATORIO: ${e.message}")
            // Fallback a notificación inmediata
            showEventReminder(event, minutesBefore, "$reminderType (error)")
        }
    }

    // 🆕 MÉTODO ACTUALIZADO PARA MOSTRAR NOTIFICACIONES (CON INFORMACIÓN DE RECORDATORIO)
    fun showEventReminder(event: PregnancyViewModel.CalendarEvent, minutesBefore: Int = 0, reminderType: String = "confirmación") {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear intent para abrir la app al hacer clic en la notificación
        val intent = Intent(context, Class.forName("com.tuusuario.creciendojuntos.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determinar icono y color según tipo de evento
        val (icon, color) = when {
            event.type == EventType.ULTRASOUND.name -> Pair(R.drawable.ic_ultrasound, R.color.blue_500)
            event.type == EventType.MEDICAL_APPOINTMENT.name -> Pair(R.drawable.ic_medical, R.color.green_500)
            event.type == EventType.PERSONAL_MILESTONE.name -> Pair(R.drawable.ic_milestone, R.color.orange_500)
            else -> Pair(R.drawable.ic_calendar, R.color.purple_500)
        }

        // 🆕 TEXTO PERSONALIZADO SEGÚN EL TIPO DE RECORDATORIO
        val reminderText = when (reminderType) {
            "30min" -> "⏰ Recordatorio: 30 minutos antes"
            "1hora" -> "⏰ Recordatorio: 1 hora antes"
            "1dia" -> "⏰ Recordatorio: 1 día antes"
            "30min (inmediato)", "1hora (inmediato)", "1dia (inmediato)" -> "📱 Recordatorio: ${reminderType.removeSuffix(" (inmediato)")} antes"
            "30min (error)", "1hora (error)", "1dia (error)" -> "⚠️ Recordatorio: ${reminderType.removeSuffix(" (error)")} antes"
            else -> "✅ Evento guardado"
        }

        // 🆕 CONSTRUIR DESCRIPCIÓN MEJORADA
        val descriptionBuilder = StringBuilder()
        if (event.description.isNotEmpty()) {
            descriptionBuilder.append(event.description)
        }
        if (minutesBefore > 0 && reminderType != "confirmación") {
            if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append("\n\n")
            descriptionBuilder.append("⏰ Recordatorio programado: $minutesBefore minutos antes")
        }

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EVENTS)
            .setSmallIcon(icon)
            .setColor(context.getColor(color))
            .setContentTitle("$reminderText: ${event.title}")
            .setContentText(if (descriptionBuilder.isNotEmpty()) descriptionBuilder.toString() else "Sin descripción")
            .setStyle(NotificationCompat.BigTextStyle().bigText(descriptionBuilder.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        // 🆕 ID ÚNICO PARA CADA NOTIFICACIÓN
        val notificationId = NOTIFICATION_ID_EVENT + event.id.toInt() + reminderType.hashCode()

        notificationManager.notify(notificationId, notification)

        println("🔔 NOTIFICACIÓN MOSTRADA: $reminderText - ${event.title}")
        println("   - ID de notificación: $notificationId")
    }

    // 🔔 FUNCIÓN PARA PRUEBAS - CORREGIDA
    fun showTestNotification() {
        // Crear un evento de prueba solo con los parámetros que existen
        val testEvent = PregnancyViewModel.CalendarEvent(
            id = 123L,
            title = "🎉 ¡Notificación de Prueba!",
            description = "Esta es una notificación de prueba para verificar que todo funciona correctamente. ¡Las notificaciones están listas!",
            date = java.time.LocalDate.now(),
            time = java.time.LocalTime.now(),
            type = EventType.PERSONAL_MILESTONE.name
        )

        // Mostrar la notificación de prueba
        showEventReminder(testEvent, 0, "prueba")

        // También puedes mostrar un Toast para confirmar
        android.widget.Toast.makeText(
            context,
            "🔔 Notificación de prueba enviada",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // 🆕 MÉTODO PARA CANCELAR RECORDATORIOS (ÚTIL CUANDO SE ELIMINA UN EVENTO)
    fun cancelReminders(event: PregnancyViewModel.CalendarEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancelar todos los tipos de recordatorios para este evento
        val reminderTypes = listOf("30min", "1hora", "1dia", "default")

        reminderTypes.forEach { reminderType ->
            val minutesBefore = when (reminderType) {
                "30min" -> REMINDER_30MIN
                "1hora" -> REMINDER_1HORA
                "1dia" -> REMINDER_1DIA
                else -> 60
            }

            val uniqueId = (event.id.toString() + reminderType + minutesBefore).hashCode()
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                println("❌ RECORDATORIO CANCELADO: ${event.title} - $reminderType")
            }
        }
    }
}