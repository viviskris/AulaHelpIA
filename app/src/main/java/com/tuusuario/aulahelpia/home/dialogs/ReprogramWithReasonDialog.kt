package com.tuusuario.aulahelpia.home.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.Spinner
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.tuusuario.aulahelpia.R
import java.time.LocalDate
import java.time.LocalTime

class ReprogramWithReasonDialog(
    private val currentDate: String,          // Fecha actual del evento
    private val currentTime: String = "09:00", // Hora actual del evento
    private val onReprogramComplete: (newDate: String, newTime: String, reason: String?) -> Unit
) : DialogFragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    // Variables para guardar selecciones temporales
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0
    private var selectedReason: String? = null

    // Lista de motivos predefinidos
    private val predefinedReasons = listOf(
        "Selecciona un motivo (opcional)",
        "⏳ Esperando información",
        "📅 Prioridad cambiada",
        "👤 Esperando a otra persona",
        "🔧 Necesita más tiempo",
        "💼 Recursos insuficientes",
        "📋 Otro motivo"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Parsear la fecha actual del evento
        val current = LocalDate.parse(currentDate)
        val year = current.year
        val month = current.monthValue - 1
        val day = current.dayOfMonth

        // Guardar valores iniciales
        selectedYear = year
        selectedMonth = month
        selectedDay = day

        // Crear DatePickerDialog
        return DatePickerDialog(requireContext(), this, year, month, day).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            setTitle("📅 Selecciona nueva fecha")
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        println("🔍 [DEBUG] onDateSet - Año: $year, Mes: $month, Día: $day")
        // Guardar fecha seleccionada
        selectedYear = year
        selectedMonth = month
        selectedDay = day

        // Parsear hora actual
        val currentTimeParsed = LocalTime.parse(currentTime)
        println("🔍 [DEBUG] Hora actual parseada: $currentTimeParsed")

        // Crear TimePickerDialog
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            this,
            currentTimeParsed.hour,
            currentTimeParsed.minute,
            true
        )

        timePickerDialog.setTitle("⏰ Selecciona nueva hora")

        // Botón para mantener hora original
        timePickerDialog.setButton(
            TimePickerDialog.BUTTON_NEUTRAL,
            "Mantener hora original",
            android.content.DialogInterface.OnClickListener { _, _ ->
                // Mostrar diálogo de motivo DESPUÉS de seleccionar hora
                showReasonDialog(year, month, day, currentTime)
            }
        )

        // Cuando se selecciona hora en el TimePicker
        timePickerDialog.setOnDismissListener {
            // Si se cerró sin usar el botón neutral, mostrar diálogo de motivo
            if (selectedReason == null) {
                showReasonDialog(year, month, day, currentTime)
            }
        }

        timePickerDialog.show()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        println("🔍 [DEBUG] onTimeSet - Hora: $hourOfDay, Minuto: $minute")

        // Guardar en variables locales
        val localYear = selectedYear
        val localMonth = selectedMonth
        val localDay = selectedDay

        // Formatear hora seleccionada
        val newTime = String.format("%02d:%02d", hourOfDay, minute)

        // Usar postDelayed para asegurar que el Fragment esté ready
        view?.postDelayed({
            println("🔍 [DEBUG] postDelayed ejecutando - isAdded: $isAdded, context: ${context != null}")

            // Verificar que todavía estemos attached
            if (isAdded && context != null) {
                println("🔍 [DEBUG] Llamando a showReasonDialog")
                showReasonDialog(localYear, localMonth, localDay, newTime)
            } else {
                println("🔍 [DEBUG] Fragment no attached - Fallback sin motivo")
                // Fallback: llamar al callback directamente sin motivo
                val newDate = LocalDate.of(localYear, localMonth + 1, localDay).toString()
                onReprogramComplete(newDate, newTime, null)
            }
        }, 100)
    }
    private fun showReasonDialog(year: Int, month: Int, day: Int, time: String) {
        try {
            // SOLUCIÓN: Usar applicationContext si el fragment no está attached
            val safeContext = context ?: requireActivity().applicationContext

            // Crear diálogo personalizado para seleccionar motivo
            val reasonDialog = Dialog(safeContext)

            // Inflar el layout manualmente
            val inflater = LayoutInflater.from(safeContext)
            val dialogView = inflater.inflate(R.layout.dialog_reprogram_with_reason, null)
            reasonDialog.setContentView(dialogView)

            // Obtener vistas con findViewById
            val spinnerReason = dialogView.findViewById<Spinner>(R.id.spinnerReason)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val adViewReprogram = dialogView.findViewById<com.google.android.gms.ads.AdView>(R.id.adViewReprogram)

            // 🆕 CARGAR BANNER
            setupDialogBanner(adViewReprogram, safeContext)

            // Configurar Spinner con motivos
            val adapter = ArrayAdapter(
                safeContext,
                android.R.layout.simple_spinner_item,
                predefinedReasons
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerReason.adapter = adapter

            // Botón Confirmar
            btnConfirm.setOnClickListener {
                val selectedPosition = spinnerReason.selectedItemPosition
                val selectedReason = if (selectedPosition > 0) {
                    predefinedReasons[selectedPosition]
                } else {
                    null
                }

                // Formatear fecha final
                val newDate = LocalDate.of(year, month + 1, day).toString()

                // Llamar al callback con todos los datos
                onReprogramComplete(newDate, time, selectedReason)

                reasonDialog.dismiss()
                this.dismiss()
            }

            // Botón Cancelar
            btnCancel.setOnClickListener {
                reasonDialog.dismiss()
                this.dismiss()
            }

            reasonDialog.show()

        } catch (e: Exception) {
            println("💥 ERROR al mostrar diálogo de motivo: ${e.message}")
            e.printStackTrace()

            // Fallback: completar sin motivo
            val newDate = LocalDate.of(year, month + 1, day).toString()
            onReprogramComplete(newDate, time, null)
            dismiss()
        }
    }
    // 🆕 FUNCIÓN PARA CARGAR BANNER EN EL DIÁLOGO
    private fun setupDialogBanner(adView: com.google.android.gms.ads.AdView, context: android.content.Context) {
        try {
            MobileAds.initialize(context)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            println("✅ Banner cargado en diálogo de reprogramación")
        } catch (e: Exception) {
            println("⚠️ Error cargando banner en diálogo: ${e.message}")
        }
    }
}