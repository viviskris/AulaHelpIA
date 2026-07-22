package com.tuusuario.aulahelpia.home.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class ReprogramCompleteDialog(
    private val originalDate: String,
    private val originalTime: String = "09:00",
    private val onReprogramComplete: (newDate: String, newTime: String, reason: String?) -> Unit
) : DialogFragment() {

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
        // Crear diálogo personalizado
        val dialog = Dialog(requireContext())

        // Inflar el layout
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_reprogram_complete, null)
        dialog.setContentView(view)

        // 🆕 CARGAR BANNER
        setupBanner(view)

        // Configurar controles
        setupDatePicker(view)
        setupTimePicker(view)
        setupReasonSpinner(view)
        setupButtons(view, dialog)

        // Estilo del diálogo
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        return dialog
    }

    // 🆕 FUNCIÓN PARA CARGAR BANNER
    private fun setupBanner(view: View) {
        try {
            // ⚠️ CAMBIA EL ID A adViewReprogram
            val adView = view.findViewById<com.google.android.gms.ads.AdView>(R.id.adViewReprogram)
            MobileAds.initialize(requireContext())
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            println("✅ Banner cargado en diálogo de reprogramación completa")
        } catch (e: Exception) {
            println("⚠️ Error cargando banner: ${e.message}")
        }
    }

    private fun setupDatePicker(view: View) {
        val datePicker = view.findViewById<DatePicker>(R.id.datePicker)

        // Parsear fecha original
        val original = LocalDate.parse(originalDate)

        // Configurar fecha mínima (hoy)
        datePicker.minDate = System.currentTimeMillis() - 1000

        // Establecer fecha original
        datePicker.updateDate(original.year, original.monthValue - 1, original.dayOfMonth)
    }

    private fun setupTimePicker(view: View) {
        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)

        // Parsear hora original
        val original = LocalTime.parse(originalTime)

        // Configurar hora original (API 23+ usa setHour/setMinute)
        timePicker.hour = original.hour
        timePicker.minute = original.minute
        timePicker.setIs24HourView(true)
    }

    private fun setupReasonSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerReason)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            predefinedReasons
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupButtons(view: View, dialog: Dialog) {
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            // Obtener valores seleccionados
            val datePicker = view.findViewById<DatePicker>(R.id.datePicker)
            val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
            val spinner = view.findViewById<Spinner>(R.id.spinnerReason)

            // Formatear nueva fecha
            val newDate = LocalDate.of(
                datePicker.year,
                datePicker.month + 1, // DatePicker usa 0-11
                datePicker.dayOfMonth
            ).toString()

            // Formatear nueva hora (API 23+ usa hour/minute)
            val newTime = String.format("%02d:%02d", timePicker.hour, timePicker.minute)

            // Obtener motivo (si se seleccionó)
            val selectedPosition = spinner.selectedItemPosition
            val reason = if (selectedPosition > 0) {
                predefinedReasons[selectedPosition]
            } else {
                null
            }

            // Llamar al callback
            onReprogramComplete(newDate, newTime, reason)

            dialog.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Ajustar tamaño del diálogo
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}