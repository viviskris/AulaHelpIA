package com.tuusuario.creciendojuntos.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.databinding.FragmentDashboardBinding
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.tuusuario.creciendojuntos.home.model.PregnancyProgress
import com.tuusuario.creciendojuntos.home.data.EventType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PregnancyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // 🆕 SOLO AGREGAR SCROLL - NO TOCAR NADA MÁS
        binding.root.isVerticalScrollBarEnabled = true
        binding.root.isScrollContainer = true

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🆕 AGREGAR ESTO AL INICIO de onViewCreated:
        // Configurar AdMob
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewDashboard.loadAd(adRequest)

        println("🎯 DASHBOARD - ONVIEWCREATED INICIADO")

        // 🆕 AGREGAR ESTA LÍNEA NUEVA
        debugSharedPreferences()

        // 🆕 FORZAR CARGA DE LA FECHA PRIMERO (TU CÓDIGO ORIGINAL)
        loadDueDateFromSharedPreferences()

        setupDashboard()

        println("🎯 DASHBOARD - ONVIEWCREATED COMPLETADO")
    }

    // 🆕 MANTENER TU MÉTODO ORIGINAL PARA CARGAR FECHA
    private fun loadDueDateFromSharedPreferences() {
        println("🔍 DASHBOARD - loadDueDateFromSharedPreferences() EJECUTÁNDOSE")

        try {
            // 🆕 CAMBIO: Cambiar "app_preferences" por "app_prefs"
            val sharedPreferences = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val savedDueDate = sharedPreferences.getString("due_date", null)

            println("📁 DASHBOARD - SharedPreferences cargado, due_date: '$savedDueDate'")

            if (savedDueDate != null && savedDueDate.isNotBlank()) {
                println("✅ DASHBOARD - Fecha encontrada en SharedPreferences: $savedDueDate")
                val dueDate = LocalDate.parse(savedDueDate)
                println("📅 DASHBOARD - Fecha parseada: $dueDate")

                viewModel.setDueDate(dueDate)
                println("🚀 DASHBOARD - ViewModel.setDueDate() llamado")

                viewModel.calculatePregnancyProgress(dueDate)
                println("🧮 DASHBOARD - ViewModel.calculatePregnancyProgress() llamado")

            } else {
                println("❌ DASHBOARD - NO hay fecha guardada en SharedPreferences")
                displaySampleData()
            }

        } catch (e: Exception) {
            println("💥 DASHBOARD - ERROR en loadDueDateFromSharedPreferences: ${e.message}")
            e.printStackTrace()
            displaySampleData()
        }
    }

    private fun setupDashboard() {
        println("⚙️ DASHBOARD - setupDashboard() INICIADO")

        setupObservers()
        setupClickListeners()
        loadInitialData()

        println("⚙️ DASHBOARD - setupDashboard() COMPLETADO")
    }

    private fun loadInitialData() {
        println("📊 DASHBOARD - loadInitialData() INICIADO")

        // 🆕 MANTENER TU VERIFICACIÓN ORIGINAL
        println("   - dueDate en ViewModel: ${viewModel.dueDate.value}")
        println("   - pregnancyProgress en ViewModel: ${viewModel.pregnancyProgress.value}")

        // 🆕 MANTENER TUS MÉTODOS ORIGINALES DE CITAS
        loadNextMedicalAppointment()
        loadWeeklyTips()

        println("📊 DASHBOARD - loadInitialData() COMPLETADO")
    }

    private fun setupObservers() {
        println("👀 DASHBOARD - setupObservers() CONFIGURANDO")

        // 🆕 MANTENER TUS OBSERVERS ORIGINALES
        viewModel.pregnancyProgress.observe(viewLifecycleOwner) { progress ->
            println("📈 DASHBOARD - OBSERVER pregnancyProgress: $progress")
            if (progress != null) {
                println("✅ DASHBOARD - Mostrando datos REALES: Semana ${progress.currentWeek}")
                updateUIWithProgress(progress)
            } else {
                println("❌ DASHBOARD - pregnancyProgress es NULL - mostrando ejemplo")
                displaySampleData()
            }
        }

        // ✅ CORREGIDO (actualiza cuando cambia la fecha):
        viewModel.dueDate.observe(viewLifecycleOwner) { dueDate ->
            println("📅 DASHBOARD - OBSERVER dueDate: $dueDate")
            dueDate?.let {
                // ¡Esto es lo que falta! Recalcular el progreso cuando cambia la fecha
                viewModel.calculatePregnancyProgress(it)
                println("🔄 DASHBOARD - Recalculando progreso con nueva fecha: $it")
            }
        }
    }

    private fun updateUIWithProgress(progress: PregnancyProgress) {
        // 🆕 MANTENER TU MÉTODO ORIGINAL COMPLETO
        // 1. 👶 SECCIÓN "TU EMBARAZO" - CORREGIDO
        binding.weekNumber.text = progress.currentWeek.toString()

        // Días de embarazo (calculamos el total)
        val totalDays = (progress.currentWeek * 7) + progress.daysInCurrentWeek
        binding.daysPregnant.text = "$totalDays días"

        // Días restantes (calculamos desde semanas restantes)
        val daysRemaining = progress.weeksRemaining * 7
        binding.daysToGo.text = "$daysRemaining días"

        // Tamaño del bebé
        binding.babySize.text = progress.babySize

        // 🆕 ACTUALIZAR CONSEJOS CON LA SEMANA ACTUAL
        updateTipsWithWeek(progress.currentWeek)

        println("📊 UI Actualizada: Semana ${progress.currentWeek}, Días: $totalDays, Tamaño: ${progress.babySize}")
    }

    // 🆕 MANTENER TU MÉTODO ORIGINAL COMPLETO PARA CITAS MÉDICAS
    private fun loadNextMedicalAppointment() {
        try {
            println("🔍 BUSCANDO PRÓXIMA CITA MÉDICA EN BASE DE DATOS...")

            val today = LocalDate.now()
            println("📅 Hoy es: $today")

            // 🆕 MANTENER TU BÚSQUEDA ORIGINAL EN 3 MESES
            val allMedicalEvents = mutableListOf<PregnancyViewModel.CalendarEvent>()
            val monthsToSearch = 3

            for (monthOffset in 0 until monthsToSearch) {
                val searchDate = today.plusMonths(monthOffset.toLong())
                val year = searchDate.year
                val month = searchDate.monthValue

                try {
                    val monthlyEvents = viewModel.getEventsForMonth(year, month, requireContext())
                    val medicalEvents = monthlyEvents.filter {
                        it.type == EventType.MEDICAL_APPOINTMENT.name
                    }

                    allMedicalEvents.addAll(medicalEvents)
                    println("📊 Mes ${searchDate.month} ($monthOffset): ${medicalEvents.size} citas médicas")

                    // 🆕 MANTENER TU LOG DETALLADO ORIGINAL
                    medicalEvents.forEach { event ->
                        println("   🏥 ${event.title} - ${event.date} ${event.time}")
                    }

                } catch (e: Exception) {
                    println("⚠️ Error buscando en mes $month/$year: ${e.message}")
                }
            }

            println("📈 Total de citas médicas encontradas: ${allMedicalEvents.size}")

            // 🆕 MANTENER TU FILTRADO Y ORDENAMIENTO ORIGINAL
            val nextAppointment = allMedicalEvents
                .filter { it.date >= today }
                .sortedWith(compareBy<PregnancyViewModel.CalendarEvent> { it.date }.thenBy { it.time })
                .firstOrNull()

            if (nextAppointment != null) {
                val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("es", "ES"))
                val formattedDate = nextAppointment.date.format(formatter)
                val formattedTime = nextAppointment.time.format(DateTimeFormatter.ofPattern("HH:mm"))

                // 🆕 MANTENER TU FORMATO ORIGINAL
                val displayText = if (nextAppointment.title.length > 30) {
                    "${nextAppointment.title.substring(0, 30)}...\n$formattedDate - $formattedTime"
                } else {
                    "${nextAppointment.title}\n$formattedDate - $formattedTime"
                }

                binding.nextAppointment.text = displayText
                println("✅ PRÓXIMA CITA ENCONTRADA: ${nextAppointment.title} - $formattedDate - $formattedTime")

            } else {
                // 🆕 MANTENER TUS MENSAJES ORIGINALES
                if (allMedicalEvents.isEmpty()) {
                    binding.nextAppointment.text = "No tienes citas médicas registradas"
                    println("💡 Sugerencia: Ve a 'Configurar Hitosa' → 'Cita Médica' para agregar una")
                } else {
                    binding.nextAppointment.text = "No hay citas médicas futuras"
                    println("ℹ️ Todas las ${allMedicalEvents.size} citas son pasadas")
                }
            }

        } catch (e: Exception) {
            println("❌ ERROR CRÍTICO en loadNextMedicalAppointment: ${e.message}")
            e.printStackTrace()
            binding.nextAppointment.text = "Consulta el calendario"
        }
    }

    // 🆕 MANTENER TU MÉTODO ORIGINAL PARA CONSEJOS
    private fun updateTipsWithWeek(currentWeek: Int) {
        val tipsText = when (currentWeek) {
            in 1..4 -> "• Toma ácido fólico diariamente\n• Evita alcohol y tabaco\n• Descansa lo suficiente\n• Programa tu primera cita prenatal"
            in 5..8 -> "• Come pequeñas porciones frecuentes\n• Mantente hidratada\n• Evita alimentos crudos\n• Comienza ejercicios suaves"
            in 9..12 -> "• Usa ropa cómoda\n• Toma siestas cortas\n• Come frutas y verduras\n• Controla las náuseas con galletas"
            in 13..16 -> "• Aumenta consumo de calcio\n• Practica yoga prenatal\n• Usa crema para estrías\n• Habla con tu bebé"
            in 17..20 -> "• Duerme de lado\n• Usa faja de soporte\n• Escucha música relajante\n• Prepara la habitación del bebé"
            in 21..24 -> "• Controla la acidez estomacal\n• Eleva los pies al descansar\n• Toma mucha agua\n• Asiste a clases prenatales"
            in 25..28 -> "• Monitorea los movimientos del bebé\n• Descansa frecuentemente\n• Prepara el plan de parto\n• Empaca tu maleta hospital"
            in 29..32 -> "• Practica respiración\n• Masajes para piernas cansadas\n• Alimentos ricos en hierro\n• Descanso es primordial"
            in 33..36 -> "• Señales de parto premarturo\n• Posiciones cómodas para dormir\n• Preparativos finales\n• Descanso total recomendado"
            in 37..40 -> "• Señales de trabajo de parto\n• Técnicas de relajación\n• Contacta a tu médico\n• ¡Estás lista para recibir a tu bebé!"
            else -> "• Descansa cuando lo necesites\n• Mantente hidratada\n• Come frutas y verduras\n• Realiza ejercicio suave"
        }

        // 🆕 MANTENER TU ACTUALIZACIÓN ORIGINAL
        binding.tipsText.text = tipsText
        println("💡 Consejos actualizados para semana $currentWeek")
    }

    // 🆕 MANTENER TU MÉTODO ORIGINAL PARA CARGAR CONSEJOS
    private fun loadWeeklyTips() {
        val currentWeek = viewModel.pregnancyProgress.value?.currentWeek ?: 12
        updateTipsWithWeek(currentWeek)
    }

    private fun setupClickListeners() {
        // 🆕 MANTENER TUS CLICK LISTENERS ORIGINALES
        binding.weekNumber.setOnClickListener {
            showWeekDetails()
        }

        // ✅ BOTÓN "VER CALENDARIO" ORIGINAL
        binding.btnViewCalendar.setOnClickListener {
            navigateToCalendar()
        }

        // Tarjeta de próxima cita (texto) ORIGINAL
        binding.nextAppointment.setOnClickListener {
            navigateToCalendar()
        }

        // 🆕 TARJETA DE CONSEJOS ORIGINAL
        binding.tipsText.setOnClickListener {
            showTipsDetails()
        }
    }

    private fun displaySampleData() {
        // 🆕 MANTENER TUS DATOS DE EJEMPLO ORIGINALES
        binding.weekNumber.text = "12"
        binding.daysPregnant.text = "84 días"
        binding.daysToGo.text = "186 días"
        binding.babySize.text = "una ciruela"
        binding.nextAppointment.text = "No hay citas programadas"

        // 🆕 MANTENER TUS CONSEJOS ORIGINALES
        updateTipsWithWeek(12)
    }

    private fun showWeekDetails() {
        android.widget.Toast.makeText(
            context,
            "Estás en la semana ${binding.weekNumber.text} de embarazo",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // 🆕 MANTENER TU MÉTODO ORIGINAL
    private fun showTipsDetails() {
        android.widget.Toast.makeText(
            context,
            "Consejos personalizados para tu semana de embarazo",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun navigateToCalendar() {
        try {
            findNavController().navigate(R.id.navigation_calendar)
            println("✅ Navegando al calendario")
        } catch (e: Exception) {
            println("❌ Error navegando al calendario: ${e.message}")
            android.widget.Toast.makeText(
                context,
                "Navegando al calendario",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 🆕 MANTENER TU ACTUALIZACIÓN ORIGINAL
        loadInitialData()
    }
    // 🆕 AGREGAR ESTO AL FINAL DEL ARCHIVO, ANTES DEL onDestroyView
    private fun debugSharedPreferences() {
        try {
            // 🆕 CAMBIO: Cambiar "app_preferences" por "app_prefs" aquí también
            val sharedPreferences = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val savedDueDate = sharedPreferences.getString("due_date", "NO_HAY_FECHA")
            val allEntries = sharedPreferences.all

            println("🔍 DEBUG SHAREDPREFERENCES:")
            println("   - due_date: '$savedDueDate'")
            println("   - Todas las entradas: $allEntries")
            println("   - ¿Está vacío?: ${sharedPreferences.all.isEmpty()}")

        } catch (e: Exception) {
            println("💥 ERROR en debug: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}