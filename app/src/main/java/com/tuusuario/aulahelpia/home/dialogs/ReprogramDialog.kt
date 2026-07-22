package com.tuusuario.aulahelpia.home.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.time.LocalDate
import java.time.LocalTime

class ReprogramDialog(
    private val currentDate: String,          // Fecha actual del evento
    private val currentTime: String = "09:00", // Hora actual del evento
    private val onDateTimeSelected: (newDate: String, newTime: String) -> Unit
) : DialogFragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    // Variables para guardar la fecha seleccionada
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Parsear la fecha actual del evento
        val current = LocalDate.parse(currentDate)
        val year = current.year
        val month = current.monthValue - 1  // Calendar usa meses 0-11
        val day = current.dayOfMonth

        // Guardar valores iniciales
        selectedYear = year
        selectedMonth = month
        selectedDay = day

        // Crear DatePickerDialog
        return DatePickerDialog(requireContext(), this, year, month, day).apply {
            // No permitir fechas pasadas
            datePicker.minDate = System.currentTimeMillis() - 1000

            // Configurar título
            setTitle("📅 Selecciona nueva fecha")
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        // Guardar la fecha seleccionada
        selectedYear = year
        selectedMonth = month
        selectedDay = day

        // Parsear la hora actual del evento
        val currentTimeParsed = LocalTime.parse(currentTime)

        // Crear TimePickerDialog para seleccionar hora
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            this,
            currentTimeParsed.hour,    // Hora actual como predeterminada
            currentTimeParsed.minute,  // Minuto actual como predeterminada
            true  // Formato 24 horas
        )

        // Configurar el TimePickerDialog
        timePickerDialog.setTitle("⏰ Selecciona nueva hora")
        timePickerDialog.setMessage("Hora actual: $currentTime")

        // Botón para mantener la hora original
        timePickerDialog.setButton(
            TimePickerDialog.BUTTON_NEUTRAL,
            "Mantener hora original",
            android.content.DialogInterface.OnClickListener { _, _ ->
                // El usuario quiere mantener la hora original
                val newDate = LocalDate.of(year, month + 1, day).toString()
                onDateTimeSelected(newDate, currentTime)
            }
        )

        // Mostrar el TimePicker
        timePickerDialog.show()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        // Formatear la fecha seleccionada
        val newDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay).toString()

        // Formatear la hora seleccionada (siempre 2 dígitos)
        val newTime = String.format("%02d:%02d", hourOfDay, minute)

        // Llamar al callback con fecha y hora nuevas
        onDateTimeSelected(newDate, newTime)
    }
}