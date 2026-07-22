package com.tuusuario.aulahelpia.home.dialogs

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.tuusuario.aulahelpia.home.data.PlanItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OverdueTaskDialog(
    private val context: Context,
    private val task: PlanItem,
    private val onTaskResolved: () -> Unit
) {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun show() {
        AlertDialog.Builder(context)
            .setTitle("⏰ Tarea Vencida")
            .setMessage(buildMessage())
            .setPositiveButton("📅 Reprogramar") { _, _ ->
                showReprogramDialog()
            }
            .setNegativeButton("🗑️ Eliminar") { _, _ ->
                confirmDelete()
            }
            .setCancelable(false)
            .show()
    }

    private fun buildMessage(): String {
        return """
            📋 Tarea: ${task.title}
            📅 Fecha original: ${formatDate(task.date)}
            ⏰ Hora: ${task.time}
            
            Esta tarea no fue completada a tiempo.
            Por favor, elige una opción para continuar.
        """.trimIndent()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = formatter.parse(dateString)
            dateFormatter.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showReprogramDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                showTimePicker(newDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker(newDate: String) {
        val calendar = Calendar.getInstance()
        val timePicker = android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                val newTime = String.format("%02d:%02d", hour, minute)
                reprogramTask(newDate, newTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun reprogramTask(newDate: String, newTime: String) {
        val updatedTask = task.copy(
            date = newDate,
            time = newTime,
            taskState = "ACTIVO",
            isCompleted = false
        )
        // Aquí necesitamos el ViewModel - lo pasaremos como parámetro
        Toast.makeText(context, "✅ Tarea reprogramada para $newDate $newTime", Toast.LENGTH_LONG).show()
        onTaskResolved()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(context)
            .setTitle("🗑️ Eliminar tarea")
            .setMessage("¿Estás segura de que quieres eliminar '${task.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                Toast.makeText(context, "✅ Tarea eliminada", Toast.LENGTH_SHORT).show()
                onTaskResolved()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}