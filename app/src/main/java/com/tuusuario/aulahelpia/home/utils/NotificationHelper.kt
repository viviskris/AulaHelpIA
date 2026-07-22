package com.tuusuario.aulahelpia.home.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.home.data.PlanItem
import com.tuusuario.aulahelpia.home.data.ModuleType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_TASKS = "task_reminders"
        const val CHANNEL_ID_EVENTS = "event_reminders"
        const val NOTIFICATION_ID_BASE = 1000

        // Constantes para recordatorios
        const val REMINDER_30MIN = 30
        const val REMINDER_1HORA = 60
        const val REMINDER_1DIA = 24 * 60

        // Formatters
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para recordatorios de tareas
            val taskChannel = NotificationChannel(
                CHANNEL_ID_TASKS,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para tareas y recordatorios importantes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // Canal para eventos generales
            val eventChannel = NotificationChannel(
                CHANNEL_ID_EVENTS,
                "Recordatorios de Eventos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para eventos y actividades"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(eventChannel)
        }
    }

    // 🎵 NUEVO MÉTODO: Obtener sonido para notificación
    private fun getNotificationSound(task: PlanItem): Uri? {
        return try {
            // Si el usuario especificó un URI de sonido, usarlo
            if (task.notificationSoundUri.isNotEmpty()) {
                Uri.parse(task.notificationSoundUri)
            } else {
                // Si no, usar sonido por defecto del sistema
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } catch (e: Exception) {
            // Fallback al sonido por defecto
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    // MÉTODO GENÉRICO PARA PROGRAMAR RECORDATORIOS DE TAREAS
    fun scheduleTaskReminder(task: PlanItem, reminderMinutesBefore: Int = 60, reminderType: String = "default") {
        println("🔔 PROGRAMANDO RECORDATORIO: ${task.title}")
        println("   - Minutos antes: $reminderMinutesBefore")
        println("   - Tipo: $reminderType")

        programarRecordatorioConAlarmManager(task, reminderMinutesBefore, reminderType)
        showTaskReminder(task, reminderMinutesBefore, reminderType)
    }

    // MÉTODO GENÉRICO PARA PROGRAMAR CON ALARMMANAGER (compatible con minSdk 24)
    private fun programarRecordatorioConAlarmManager(task: PlanItem, minutesBefore: Int, reminderType: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // ✅ PRIMERO: Verificar permiso para alarmas exactas (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    println("⚠️ No hay permiso para alarmas exactas")
                    showTaskReminder(task, minutesBefore, "$reminderType (permiso denegado)")
                    return
                }
            }

            // Parsear fecha y hora de la tarea (compatible con minSdk 24)
            val taskDate = dateFormatter.parse(task.date) ?: Date()
            val taskTimeStr = if (task.time.isNotEmpty()) task.time else "09:00"

            // Crear Calendar con fecha y hora de la tarea
            val taskCalendar = Calendar.getInstance().apply {
                time = taskDate
                // Parsear la hora (formato HH:mm)
                val timeParts = taskTimeStr.split(":")
                if (timeParts.size >= 2) {
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                } else {
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Restar minutos para el recordatorio
            val reminderCalendar = taskCalendar.clone() as Calendar
            reminderCalendar.add(Calendar.MINUTE, -minutesBefore)

            val reminderTimeInMillis = reminderCalendar.timeInMillis

            // Logs para depuración
            println("⏰ TAREA: ${task.title}")
            println("📅 FECHA TAREA: ${task.date} ${task.time}")
            println("⏰ RECORDATORIO PROGRAMADO PARA: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(reminderTimeInMillis))}")
            println("📊 MINUTOS ANTES: $minutesBefore")
            println("🕐 HORA ACTUAL: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")

            // Crear Intent único
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("task_title", task.title)
                putExtra("task_description", task.description)
                putExtra("task_type", task.moduleType.name)
                putExtra("reminder_type", reminderType)
                putExtra("task_time", task.time)
            }

            val uniqueId = (task.id.toString() + reminderType + minutesBefore).hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeInMillis,
                        pendingIntent
                    )
                }
                else -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeInMillis,
                        pendingIntent
                    )
                }
            }

            println("✅ RECORDATORIO PROGRAMADO: ${task.title}")

        } catch (e: SecurityException) {
            println("⚠️ PERMISO DE ALARMA DENEGADO: ${e.message}")
            showTaskReminder(task, minutesBefore, "$reminderType (inmediato)")

        } catch (e: Exception) {
            println("❌ ERROR PROGRAMANDO RECORDATORIO: ${e.message}")
            showTaskReminder(task, minutesBefore, "$reminderType (error)")
        }
    }

    // MÉTODO GENÉRICO PARA MOSTRAR NOTIFICACIONES DE TAREAS
    fun showTaskReminder(task: PlanItem, minutesBefore: Int = 0, reminderType: String = "confirmación") {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir la app
        val intent = Intent(context, Class.forName("com.tuusuario.aulahelpia.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determinar icono y color según tipo de módulo (usando enum)
        val (icon, color) = when (task.moduleType) {
            ModuleType.TASK -> Pair(R.drawable.ic_calendar, R.color.primary_pastel)
            ModuleType.EXERCISE -> Pair(android.R.drawable.ic_media_play, R.color.completed_green)
            ModuleType.NUTRITION -> Pair(android.R.drawable.ic_menu_edit, R.color.pending_orange)
            ModuleType.STUDY -> Pair(android.R.drawable.ic_menu_help, R.color.purple_neon)
            ModuleType.PERSONAL -> Pair(android.R.drawable.ic_menu_my_calendar, R.color.purple_neon)
            ModuleType.IMPORTANT -> Pair(android.R.drawable.star_big_on, R.color.error)
        }

        // Texto personalizado según el tipo de recordatorio
        val reminderText = when (reminderType) {
            "30min" -> "⏰ Recordatorio: 30 minutos antes"
            "1hora" -> "⏰ Recordatorio: 1 hora antes"
            "1dia" -> "⏰ Recordatorio: 1 día antes"
            else -> "✅ Tarea guardada"
        }

        // Construir descripción
        val descriptionBuilder = StringBuilder()
        if (task.description.isNotEmpty()) {
            descriptionBuilder.append(task.description)
        }
        if (minutesBefore > 0 && reminderType != "confirmación") {
            if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append("\n\n")
            descriptionBuilder.append("⏰ Recordatorio programado: $minutesBefore minutos antes")
        }

        // 🎵 OBTENER SONIDO
        val soundUri = getNotificationSound(task)
        println("🔊 SONIDO: ${task.notificationSoundUri} -> $soundUri")

        // Construir notificación
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_TASKS)
            .setSmallIcon(icon)
            .setColor(context.getColor(color))
            .setContentTitle("$reminderText: ${task.title}")
            .setContentText(if (descriptionBuilder.isNotEmpty()) descriptionBuilder.toString() else "Sin descripción")
            .setStyle(NotificationCompat.BigTextStyle().bigText(descriptionBuilder.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Agregar vibración si está habilitada
        if (task.notificationVibration) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        // Agregar sonido si hay URI
        soundUri?.let {
            notificationBuilder.setSound(it)
        }

        val notification = notificationBuilder.build()
        val notificationId = NOTIFICATION_ID_BASE + task.id.toInt() + reminderType.hashCode()
        notificationManager.notify(notificationId, notification)

        println("🔔 NOTIFICACIÓN MOSTRADA: $reminderText - ${task.title}")
    }

    // MÉTODO DE PRUEBA GENÉRICO (compatible)
    fun showTestNotification() {
        val testTask = PlanItem(
            id = 123L,
            title = "🎉 ¡Notificación de Prueba!",
            description = "Esta es una notificación de prueba para verificar que todo funciona correctamente.",
            date = dateFormatter.format(Date()),
            time = "09:00",
            moduleType = ModuleType.TASK // ✅ Usando enum
        )

        showTaskReminder(testTask, 0, "prueba")

        android.widget.Toast.makeText(
            context,
            "🔔 Notificación de prueba enviada",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // MÉTODO PARA CANCELAR RECORDATORIOS
    fun cancelReminders(task: PlanItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminderTypes = listOf("30min", "1hora", "1dia", "default")

        reminderTypes.forEach { reminderType ->
            val minutesBefore = when (reminderType) {
                "30min" -> REMINDER_30MIN
                "1hora" -> REMINDER_1HORA
                "1dia" -> REMINDER_1DIA
                else -> 60
            }

            val uniqueId = (task.id.toString() + reminderType + minutesBefore).hashCode()
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
                println("❌ RECORDATORIO CANCELADO: ${task.title} - $reminderType")
            }
        }
    }
}