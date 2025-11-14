package com.tuusuario.creciendojuntos.onboarding.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tuusuario.creciendojuntos.databinding.FragmentDueDateBinding
import com.tuusuario.creciendojuntos.onboarding.OnboardingActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class DueDateFragment : Fragment() {

    private var _binding: FragmentDueDateBinding? = null
    private val binding get() = _binding!!

    private var selectedDueDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDueDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ AGREGA ESTA LÍNEA (debe ser IDÉNTICA a la del CalendarFragment):

        setupDatePicker()
        setupClickListeners()
        updateSelectedDateDisplay()
    }

    private fun setupDatePicker() {
        // Configurar el date picker para seleccionar fecha probable de parto
        val today = LocalDate.now()
        val minDate = today.minusMonths(1) // Permitir seleccionar hasta 1 mes atrás
        val maxDate = today.plusYears(1)   // Permitir seleccionar hasta 1 año adelante

        // Convertir a milisegundos para el DatePicker
        val minDateMs = minDate.atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli()
        val maxDateMs = maxDate.atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli()

        binding.datePicker.minDate = minDateMs
        binding.datePicker.maxDate = maxDateMs

        // Establecer fecha por defecto (40 semanas desde hoy)
        val defaultDueDate = today.plusWeeks(40)
        setDatePickerDate(defaultDueDate)
        selectedDueDate = defaultDueDate
    }

    private fun setDatePickerDate(date: LocalDate) {
        binding.datePicker.updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
    }

    private fun setupClickListeners() {
        // ✅ CORREGIDO: Usar init en lugar de setOnDateChangedListener
        binding.datePicker.init(
            binding.datePicker.year,
            binding.datePicker.month,
            binding.datePicker.dayOfMonth
        ) { _, year, month, day ->
            selectedDueDate = LocalDate.of(year, month + 1, day)
            updateSelectedDateDisplay()
        }

        binding.btnContinue.setOnClickListener {
            selectedDueDate?.let { dueDate ->
                navigateToNext(dueDate)
            }
        }

        binding.btnSkip.setOnClickListener {
            // Usar fecha por defecto
            val defaultDueDate = LocalDate.now().plusWeeks(40)
            navigateToNext(defaultDueDate)
        }
    }

    private fun updateSelectedDateDisplay() {
        selectedDueDate?.let { date ->
            val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")
            val formattedDate = date.format(formatter)
            binding.selectedDateText.text = "Fecha seleccionada: $formattedDate"
            binding.selectedDateText.visibility = View.VISIBLE

            // Calcular y mostrar semanas aproximadas
            val today = LocalDate.now()
            val weeksPregnant = 40 - java.time.temporal.ChronoUnit.WEEKS.between(date, today.plusWeeks(40)).toInt()
            binding.weeksPregnantText.text = "Aproximadamente ${weeksPregnant.coerceIn(1, 40)} semanas"
            binding.weeksPregnantText.visibility = View.VISIBLE
        }
    }

    private fun navigateToNext(dueDate: LocalDate) {
        // ✅ CORRECTO: Esta parte está bien - llama al método correcto
        val activity = activity as? OnboardingActivity
        activity?.completeOnboardingWithDate(dueDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}