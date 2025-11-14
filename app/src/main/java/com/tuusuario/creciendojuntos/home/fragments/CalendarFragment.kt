package com.tuusuario.creciendojuntos.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.databinding.FragmentCalendarBinding
import com.tuusuario.creciendojuntos.home.data.EventType
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.tuusuario.creciendojuntos.home.utils.NotificationHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.temporal.ChronoUnit
import android.os.Handler
import android.os.Looper


class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PregnancyViewModel by viewModels()

    private var currentMonth = YearMonth.now()
    private var selectedDateForEvent: LocalDate = LocalDate.now()

    // 🆕 VARIABLES PARA BÚSQUEDA (SOLO PARA FILTRAR VISUALMENTE)
    private var textoBusqueda = ""
    private val tiposSeleccionados = mutableSetOf<String>("Todos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar AdMob
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewCalendar.loadAd(adRequest)

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(toolbar)

        loadDueDateFromSharedPreferences()

        setupCalendar()
        setupObservers()
        configurarBusquedaYFiltros()

        // 🆕 INICIALIZAR LA INFORMACIÓN DEL DASHBOARD
        displayPregnancyMilestones()

        // 🆕 ACTUALIZAR SEMANA ACTUAL SI HAY DATOS
        viewModel.pregnancyProgress.value?.let { progress ->
            binding.currentWeekText.text = "Semana actual: ${progress.currentWeek}"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.calendar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_config_hitos -> {
                findNavController().navigate(R.id.navigation_hitos_config)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 🆕 MÉTODOS DE BÚSQUEDA Y FILTROS (SOLO FILTRAN VISUALMENTE)
    private fun configurarBusquedaYFiltros() {
        // Configurar SearchView
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                textoBusqueda = newText?.lowercase() ?: ""
                generateCalendarGrid() // 🆕 ACTUALIZAR CALENDARIO CON FILTROS
                return true
            }
        })

        // Configurar Chips
        val chipIds = listOf(R.id.chipTodos, R.id.chipSalud, R.id.chipEducacion, R.id.chipEntretenimiento)
        chipIds.forEach { chipId ->
            val chip = requireView().findViewById<com.google.android.material.chip.Chip>(chipId)
            chip.setOnCheckedChangeListener { button, isChecked ->
                if (isChecked) {
                    if (chipId == R.id.chipTodos) {
                        tiposSeleccionados.clear()
                        tiposSeleccionados.add("Todos")
                        deseleccionarOtrosChips(chipId)
                    } else {
                        tiposSeleccionados.remove("Todos")
                        val chipTodos = requireView().findViewById<com.google.android.material.chip.Chip>(R.id.chipTodos)
                        chipTodos?.isChecked = false

                        val tipo = obtenerTipoDeChip(chipId)
                        tiposSeleccionados.add(tipo)
                    }
                } else {
                    val tipo = obtenerTipoDeChip(chipId)
                    tiposSeleccionados.remove(tipo)

                    if (tiposSeleccionados.isEmpty()) {
                        val chipTodos = requireView().findViewById<com.google.android.material.chip.Chip>(R.id.chipTodos)
                        chipTodos?.isChecked = true
                    }
                }
                generateCalendarGrid() // 🆕 ACTUALIZAR CALENDARIO CON FILTROS
            }
        }
    }

    private fun obtenerTipoDeChip(chipId: Int): String {
        return when (chipId) {
            R.id.chipSalud -> "Salud"
            R.id.chipEducacion -> "Educación"
            R.id.chipEntretenimiento -> "Entretenimiento"
            else -> "Todos"
        }
    }

    private fun obtenerTipoDisplay(eventType: String): String {
        return when (eventType) {
            EventType.ULTRASOUND.name, EventType.MEDICAL_APPOINTMENT.name -> "Salud"
            EventType.PERSONAL_MILESTONE.name -> "Educación"
            else -> "Entretenimiento"
        }
    }

    private fun deseleccionarOtrosChips(chipIdExcluir: Int) {
        val chipIds = listOf(R.id.chipSalud, R.id.chipEducacion, R.id.chipEntretenimiento)
        chipIds.forEach { id ->
            if (id != chipIdExcluir) {
                val chip = requireView().findViewById<com.google.android.material.chip.Chip>(id)
                chip?.isChecked = false
            }
        }
    }

    // 🆕 MÉTODO PARA OBTENER EVENTOS FILTRADOS
    private fun getEventosFiltradosParaFecha(date: LocalDate): List<PregnancyViewModel.CalendarEvent> {
        val todosEventos = viewModel.getEventsForDate(date, requireContext())

        // Si no hay filtros activos, devolver todos los eventos
        if (textoBusqueda.isEmpty() && tiposSeleccionados.contains("Todos")) {
            return todosEventos
        }

        // Aplicar filtros
        return todosEventos.filter { evento ->
            val coincideTexto = evento.title.lowercase().contains(textoBusqueda) ||
                    evento.description.lowercase().contains(textoBusqueda)

            val coincideTipo = tiposSeleccionados.contains("Todos") ||
                    tiposSeleccionados.contains(obtenerTipoDisplay(evento.type))

            coincideTexto && coincideTipo
        }
    }

    // 🆕 MÉTODO CORREGIDO PARA PROGRAMAR RECORDATORIOS
    private fun programarRecordatorios(event: PregnancyViewModel.CalendarEvent, recordatorio30min: Boolean, recordatorio1hora: Boolean, recordatorio1dia: Boolean) {
        println("🔔 CONFIGURANDO RECORDATORIOS PARA: ${event.title}")
        println("   - 30min antes: $recordatorio30min")
        println("   - 1hora antes: $recordatorio1hora")
        println("   - 1día antes: $recordatorio1dia")

        val notificationHelper = NotificationHelper(requireContext())

        // 🆕 PROGRAMAR RECORDATORIOS REALES SEGÚN LAS OPCIONES SELECCIONADAS
        if (recordatorio30min) {
            notificationHelper.scheduleEventReminder(event, 30, "30min")
            println("✅ Recordatorio de 30min programado")
        }

        if (recordatorio1hora) {
            notificationHelper.scheduleEventReminder(event, 60, "1hora")
            println("✅ Recordatorio de 1hora programado")
        }

        if (recordatorio1dia) {
            notificationHelper.scheduleEventReminder(event, 24 * 60, "1dia")
            println("✅ Recordatorio de 1dia programado")
        }

        // ✅ LA NOTIFICACIÓN EN EL MOMENTO DEL EVENTO (60min) SIGUE FUNCIONANDO
        // (ya está incluida en viewModel.addEvent())

        println("🎯 TODOS LOS RECORDATORIOS CONFIGURADOS PARA: ${event.title}")
    }

    // 🆕 AGREGAR ESTE MÉTODO NUEVO AQUÍ (después de las variables y antes de setupCalendar)
    private fun loadDueDateFromSharedPreferences() {
        println("🔍 CALENDAR - loadDueDateFromSharedPreferences() EJECUTÁNDOSE")
        try {
            val sharedPreferences = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val savedDueDate = sharedPreferences.getString("due_date", null)
            val savedLastPeriod = sharedPreferences.getString("last_period_date", null)

            println("📁 CALENDAR - SharedPreferences cargado:")
            println("   - due_date: '$savedDueDate'")
            println("   - last_period_date: '$savedLastPeriod'")

            if (savedDueDate != null && savedDueDate.isNotBlank()) {
                println("✅ CALENDAR - Fecha de parto encontrada: $savedDueDate")
                val dueDate = LocalDate.parse(savedDueDate)
                viewModel.setDueDate(dueDate)
            }

            if (savedLastPeriod != null && savedLastPeriod.isNotBlank()) {
                println("✅ CALENDAR - Fecha última regla encontrada: $savedLastPeriod")
                val lastPeriod = LocalDate.parse(savedLastPeriod)
                viewModel.setLastPeriodDate(lastPeriod)
            }
        } catch (e: Exception) {
            println("💥 CALENDAR - ERROR: ${e.message}")
            e.printStackTrace()
        }
    }

    // MÉTODOS EXISTENTES (SIN MODIFICACIONES, SOLO USAN getEventosFiltradosParaFecha)
    private fun     setupCalendar() {
        updateCalendarHeader()
        setupClickListeners()
        displayPregnancyMilestones()
    }

    private fun setupObservers() {
        viewModel.dueDate.observe(viewLifecycleOwner) { dueDate ->
            dueDate?.let {
                println("📅 OBSERVER - DueDate actualizado: $it")
                updateCalendarWithDueDate(it)
                generateCalendarGrid()
                // 🆕 ACTUALIZAR HITOS CUANDO CAMBIA LA FECHA
                displayPregnancyMilestones()
            }
        }

        viewModel.pregnancyProgress.observe(viewLifecycleOwner) { progress ->
            progress?.let {
                println("📊 OBSERVER - Progress actualizado: Semana ${it.currentWeek}")
                updateCurrentWeekMarker(it.currentWeek)
                // 🆕 ACTUALIZAR SEMANA ACTUAL EN EL TEXTO
                binding.currentWeekText.text = "Semana actual: ${it.currentWeek}"
            }
        }

        viewModel.events.observe(viewLifecycleOwner) { events ->
            println("🔔 OBSERVER - Eventos actualizados: ${events.size} eventos")
            generateCalendarGrid()
        }

        // 🆕 ELIMINAR ESTE OBSERVER DUPLICADO - YA ESTÁ ARRIBA
        // viewModel.dueDate.observe(viewLifecycleOwner) {
        //     generateCalendarGrid()
        // }
    }

    private fun updateCalendarWithDueDate(dueDate: LocalDate) {
        binding.dueDateText.text = "Fecha probable de parto: ${formatDate(dueDate)}"
        calculateAndDisplayMilestones(dueDate)
    }

    private fun updateCalendarHeader() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
        binding.calendarTitle.text = currentMonth.format(formatter)
        generateCalendarGrid()
    }

    private fun generateCalendarGrid() {
        println("🔄 INICIANDO generateCalendarGrid() CON GRIDVIEW")

        val daysOfWeek = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val weekDaysAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, daysOfWeek)
        binding.gvWeekDays.adapter = weekDaysAdapter

        val firstDay = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val startOffset = firstDay.dayOfWeek.value - 1

        val calendarDays = mutableListOf<String>()

        for (i in 0 until startOffset) {
            calendarDays.add("")
        }

        for (day in 1..daysInMonth) {
            calendarDays.add(day.toString())
        }

        val daysAdapter = object : BaseAdapter() {
            override fun getCount(): Int = calendarDays.size
            override fun getItem(position: Int): Any = calendarDays[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val dayView = convertView ?: LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_calendar_day, parent, false)

                val dayTextView = dayView.findViewById<TextView>(R.id.tvDayNumber)
                val day = calendarDays[position]

                dayTextView.text = day

                if (day.isNotEmpty()) {
                    val currentDate = currentMonth.atDay(day.toInt())
                    highlightSpecialDates(dayView, currentDate)

                    dayView.setOnClickListener {
                        println("🎯 DÍA TOCADO: $currentDate")
                        onDaySelected(currentDate)
                    }
                } else {
                    dayView.setOnClickListener(null)
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.darker_gray))
                }

                return dayView
            }
        }

        binding.gvCalendarDays.adapter = daysAdapter
        println("✅ GridView configurado: ${calendarDays.size} celdas")
    }

    private fun highlightSpecialDates(dayView: View, date: LocalDate) {
        val events = getEventosFiltradosParaFecha(date)
        val dayTextView = dayView.findViewById<TextView>(R.id.tvDayNumber)
        val today = LocalDate.now()

        println("=== DEBUG highlightSpecialDates ===")
        println("📅 Fecha: $date")
        println("🎯 Eventos encontrados: ${events.size}")
        println("🔍 Filtros activos: texto='$textoBusqueda', tipos=$tiposSeleccionados")

        // 🟢 PRIMERO verificar hitos del embarazo (SOLO si NO hay eventos)
        var isPregnancyMilestone = false
        viewModel.dueDate.value?.let { dueDate ->
            val startDate = dueDate.minusWeeks(40)
            println("📊 DueDate configurado: $dueDate")

            when {
                date == dueDate -> {
                    println("🟣 APLICANDO COLOR FECHA PARTO")
                    dayView.setBackgroundResource(R.drawable.circle_due_date)
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.white))
                    isPregnancyMilestone = true
                }
                date == startDate.plusWeeks(12) -> {
                    println("⭐ APLICANDO COLOR HITO 12 SEMANAS")
                    dayView.setBackgroundResource(R.drawable.circle_milestone)
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.white))
                    isPregnancyMilestone = true
                }
                date == startDate.plusWeeks(20) -> {
                    println("📊 APLICANDO COLOR ECOGRAFÍA AUTOMÁTICA")
                    dayView.setBackgroundResource(R.drawable.circle_ultrasound)
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.white))
                    isPregnancyMilestone = true
                }
            }
        }

        // 🟢 LUEGO verificar eventos (solo si NO es un hito del embarazo)
        if (!isPregnancyMilestone) {
            val mainEventType = getMainEventTypeForDate(events)
            println("🎨 Tipo principal determinado: $mainEventType")

            when {
                date == today -> {
                    println("🟢 APLICANDO COLOR HOY")
                    dayView.setBackgroundResource(R.drawable.circle_today)
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.white))
                }
                events.isNotEmpty() -> {
                    println("🎨 APLICANDO COLOR POR EVENTOS")
                    when (mainEventType) {
                        EventType.ULTRASOUND -> {
                            println("🔴 APLICANDO COLOR ECOGRAFÍA")
                            dayView.setBackgroundResource(R.drawable.circle_ultrasound)
                        }
                        EventType.MEDICAL_APPOINTMENT -> {
                            println("🔵 APLICANDO COLOR CITA MÉDICA")
                            dayView.setBackgroundResource(R.drawable.circle_medical)
                        }
                        EventType.PERSONAL_MILESTONE -> {
                            println("🟠 APLICANDO COLOR HITO PERSONAL")
                            dayView.setBackgroundResource(R.drawable.circle_personal_milestone)
                        }
                        else -> {
                            println("🟠 APLICANDO COLOR EVENTO PERSONAL")
                            dayView.setBackgroundResource(R.drawable.circle_personal_event)
                        }
                    }
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.white))
                }
                else -> {
                    println("⚫ NO HAY EVENTOS - COLOR NORMAL")
                    dayView.setBackgroundResource(0)
                    dayTextView.setTextColor(requireContext().getColor(android.R.color.black))
                }
            }
        }

        println("=== FIN DEBUG ===\n")
    }

    private fun getMainEventTypeForDate(events: List<PregnancyViewModel.CalendarEvent>): EventType? {
        if (events.isEmpty()) return null

        return when {
            events.any { it.type == EventType.ULTRASOUND.name } -> EventType.ULTRASOUND
            events.any { it.type == EventType.MEDICAL_APPOINTMENT.name } -> EventType.MEDICAL_APPOINTMENT
            events.any { it.type == EventType.PERSONAL_MILESTONE.name } -> EventType.PERSONAL_MILESTONE
            else -> EventType.PERSONAL
        }
    }

    private fun updateCurrentWeekMarker(currentWeek: Int) {
        binding.currentWeekText.text = "Semana actual: $currentWeek"
    }

    private fun calculateAndDisplayMilestones(dueDate: LocalDate) {
        val milestones = mutableListOf<String>()
        val startDate = dueDate.minusWeeks(40)
        val today = LocalDate.now()

        milestones.add("• Inicio del embarazo: ${formatDate(startDate)}")
        milestones.add("• Fin primer trimestre (12 semanas): ${formatDate(startDate.plusWeeks(12))}")
        milestones.add("• Ecografía morfológica (20 semanas): ${formatDate(startDate.plusWeeks(20))}")
        milestones.add("• Fin segundo trimestre (27 semanas): ${formatDate(startDate.plusWeeks(27))}")
        milestones.add("• Tercer trimestre (28 semanas): ${formatDate(startDate.plusWeeks(28))}")
        milestones.add("• ¡Fecha de parto! (40 semanas): ${formatDate(dueDate)}")

        val nextMilestone = calculateNextMilestone(startDate, today)
        milestones.add(0, "Próximo hito: $nextMilestone")

        displayMilestones(milestones)
    }

    private fun calculateNextMilestone(startDate: LocalDate, today: LocalDate): String {
        val milestones = listOf(
            12 to "Fin primer trimestre",
            20 to "Ecografía morfológica",
            27 to "Fin segundo trimestre",
            28 to "Tercer trimestre",
            40 to "¡Fecha de parto!"
        )

        for ((weeks, description) in milestones) {
            val milestoneDate = startDate.plusWeeks(weeks.toLong())
            if (today.isBefore(milestoneDate)) {
                val weeksToGo = java.time.temporal.ChronoUnit.WEEKS.between(today, milestoneDate)
                return "$description en $weeksToGo semanas"
            }
        }
        return "¡Felicidades, has completado tu embarazo!"
    }

    private fun displayPregnancyMilestones() {
        // 🆕 MEJORADO: Usar el mismo patrón que DashboardFragment - lastPeriodDate como fallback
        when {
            viewModel.dueDate.value != null -> {
                // ✅ TENEMOS FECHA DE PARTO - calcular desde ahí (COMPORTAMIENTO ORIGINAL)
                viewModel.dueDate.value?.let { dueDate ->
                    calculateAndDisplayMilestones(dueDate)
                }
            }
            viewModel.lastPeriodDate.value != null -> {
                // ✅ TENEMOS FECHA ÚLTIMA REGLA - calcular y mostrar info (NUEVO FALLBACK)
                viewModel.lastPeriodDate.value?.let { lastPeriod ->
                    calculateAndDisplayFromLastPeriod(lastPeriod)
                }
            }
            else -> {
                // ❌ NO HAY DATOS - mostrar mensaje onboarding (COMPORTAMIENTO ORIGINAL)
                binding.milestonesText.text = "Completa el onboarding para ver tu calendario personalizado"
            }
        }
    }

    // 🆕 NUEVO MÉTODO: Calcular y mostrar desde última regla (PATRÓN SEGURO)
    private fun calculateAndDisplayFromLastPeriod(lastPeriod: LocalDate) {
        val today = LocalDate.now()

        // Calcular FPP (misma lógica que el ViewModel)
        val dueDate = lastPeriod.plusDays(280)

        // Calcular semanas y días (misma lógica que el ViewModel)
        val weeksPregnant = ChronoUnit.WEEKS.between(lastPeriod, today).toInt()
        val daysPregnant = ChronoUnit.DAYS.between(
            lastPeriod.plusWeeks(weeksPregnant.toLong()),
            today
        ).toInt()

        // Mostrar información básica del embarazo
        val milestones = mutableListOf<String>()

        milestones.add("• Fecha probable de parto: ${formatDate(dueDate)}")
        milestones.add("• Semana actual: ${weeksPregnant} semanas y ${daysPregnant} días")
        milestones.add("• Tamaño del bebé: ${getBabySizeForWeek(weeksPregnant)}")
        milestones.add("• Inicio del embarazo: ${formatDate(lastPeriod)}")

        // 🆕 MANTENER LOS HITOS IMPORTANTES (igual que el método original)
        milestones.add("• Fin primer trimestre (12 semanas): ${formatDate(lastPeriod.plusWeeks(12))}")
        milestones.add("• Ecografía morfológica (20 semanas): ${formatDate(lastPeriod.plusWeeks(20))}")
        milestones.add("• Tercer trimestre (28 semanas): ${formatDate(lastPeriod.plusWeeks(28))}")
        milestones.add("• ¡Fecha de parto! (40 semanas): ${formatDate(dueDate)}")

        displayMilestones(milestones)
    }

    // 🆕 MÉTODO AUXILIAR: Obtener tamaño del bebé (igual que el ViewModel)
    private fun getBabySizeForWeek(week: Int): String {
        return when (week) {
            in 1..4 -> "Semilla de amapola"
            in 5..8 -> "Frambuesa"
            in 9..12 -> "Lima"
            in 13..16 -> "Aguacate"
            in 17..20 -> "Pimiento"
            in 21..24 -> "Maíz"
            in 25..28 -> "Berenjena"
            in 29..32 -> "Calabacín"
            in 33..36 -> "Lechuga romana"
            in 37..40 -> "Sandía"
            else -> "Pequeño milagro"
        }
    }

    private fun displayMilestones(milestones: List<String>) {
        val milestonesText = milestones.joinToString("\n")
        binding.milestonesText.text = milestonesText
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))
        return date.format(formatter)
    }

    private fun setupClickListeners() {
        binding.prevMonthButton.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateCalendarHeader()
        }

        binding.nextMonthButton.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateCalendarHeader()
        }

        binding.addEventButton.setOnClickListener {
            addCustomEvent()
        }
    }

    private fun onDaySelected(date: LocalDate) {
        selectedDateForEvent = date

        // 🆕 USAR getEventosFiltradosParaFecha PARA FILTRAR
        val events = getEventosFiltradosParaFecha(date)
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val formattedDate = date.format(formatter)

        if (events.isNotEmpty()) {
            val eventDetails = events.joinToString("\n\n") { event ->
                val hora = " - ${event.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                "• ${event.title}$hora" +
                        if (event.description.isNotEmpty()) "\n  ${event.description}" else ""
            }

            val dialogBuilder = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Eventos para $formattedDate")
                .setMessage(eventDetails)
                .setPositiveButton("Agregar Evento") { _, _ ->
                    addCustomEvent()
                }
                .setNeutralButton("Cerrar", null)

            if (events.isNotEmpty()) {
                println("🔄 AGREGANDO BOTÓN ELIMINAR...")
                println("📋 EVENTOS QUE SE PASARÁN A ELIMINAR: ${events.size}")
                events.forEach {
                    println("   - ${it.title} (ID: ${it.id})")
                }

                dialogBuilder.setNegativeButton("Eliminar Evento") { _, _ ->
                    println("✅ BOTÓN ELIMINAR PRESIONADO")
                    println("📋 EVENTOS AL MOMENTO DE PRESIONAR: ${events.size}")
                    mostrarDialogoEliminarEvento(events, formattedDate)
                }
            }

            dialogBuilder.show()
        } else {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(formattedDate)
                .setMessage("No hay eventos para este día")
                .setPositiveButton("Agregar Evento") { _, _ ->
                    addCustomEvent()
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }
    }

    // 🆕 MÉTODO ORIGINAL PARA ELIMINAR (SIN MODIFICACIONES)
    private fun mostrarDialogoEliminarEvento(events: List<PregnancyViewModel.CalendarEvent>, fecha: String) {
        if (events.isEmpty()) {
            Toast.makeText(requireContext(), "No hay eventos para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        val items = arrayOfNulls<CharSequence>(events.size)
        events.forEachIndexed { index, event ->
            items[index] = event.title
        }

        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Evento")
            .setItems(items) { dialog, which ->
                val eventoAEliminar = events[which]
                confirmarEliminacion(eventoAEliminar)
            }
            .setNegativeButton("Cancelar", null)

        builder.show()
    }

    // 🆕 MÉTODO ORIGINAL PARA CONFIRMAR ELIMINACIÓN (SIN MODIFICACIONES)
    private fun confirmarEliminacion(evento: PregnancyViewModel.CalendarEvent) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Evento")
            .setMessage("¿Estás segura de que quieres eliminar '${evento.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                println("🗑️ ELIMINANDO EVENTO:")
                println("   ID: ${evento.id}")
                println("   Título: ${evento.title}")
                println("   Fecha: ${evento.date}")
                println("   Hora: ${evento.time}")

                viewModel.deleteEvent(evento.id, requireContext())

                val eventsAfterDelete = viewModel.getEventsForDate(evento.date, requireContext())
                println("📊 EVENTOS DESPUÉS DE ELIMINAR: ${eventsAfterDelete.size}")
                eventsAfterDelete.forEach {
                    println("   - ${it.title} (ID: ${it.id})")
                }

                Toast.makeText(requireContext(), "Evento eliminado", Toast.LENGTH_SHORT).show()
                generateCalendarGrid()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addCustomEvent() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event_with_time, null)
        val eventTitleEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.eventTitleEditText)
        val eventDescriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.eventDescriptionEditText)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)

        // 🆕 OBTENER CHECKBOXES DE RECORDATORIOS
        val checkRecordatorio30min = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkRecordatorio30min)
        val checkRecordatorio1hora = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkRecordatorio1hora)
        val checkRecordatorio1dia = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkRecordatorio1dia)

        // 🆕 🧪 AGREGAR ESTOS LOGS DE DIAGNÓSTICO AQUÍ 🧪
        println("🔍 DIAGNÓSTICO CHECKBOXES:")
        println("   - checkRecordatorio30min: ${checkRecordatorio30min.isChecked}")
        println("   - checkRecordatorio1hora: ${checkRecordatorio1hora.isChecked}")
        println("   - checkRecordatorio1dia: ${checkRecordatorio1dia.isChecked}")

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))
        dialogTitle.text = "Agregar Evento para ${selectedDateForEvent.format(dateFormatter)}"

        timePicker.setIs24HourView(true)
        timePicker.hour = 9
        timePicker.minute = 0

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton).setOnClickListener {
            val title = eventTitleEditText.text.toString().trim()
            val description = eventDescriptionEditText.text.toString().trim()
            val selectedTime = LocalTime.of(timePicker.hour, timePicker.minute)

            if (title.isNotEmpty()) {
                val eventType = when {
                    title.contains("ecografía", ignoreCase = true) -> EventType.ULTRASOUND.name
                    title.contains("cita", ignoreCase = true) || title.contains("médic", ignoreCase = true) -> EventType.MEDICAL_APPOINTMENT.name
                    title.contains("hito", ignoreCase = true) || title.contains("personal", ignoreCase = true) -> EventType.PERSONAL_MILESTONE.name
                    else -> EventType.PERSONAL.name
                }

                val event = PregnancyViewModel.CalendarEvent(
                    title = title,
                    description = description,
                    date = selectedDateForEvent,
                    time = selectedTime,
                    type = eventType
                )

                println("💾 GUARDANDO EVENTO: '$title' - Hora: $selectedTime - Fecha: $selectedDateForEvent")

                // 🆕 PROGRAMAR RECORDATORIOS (SOLO LOGS POR AHORA)
                programarRecordatorios(event, checkRecordatorio30min.isChecked, checkRecordatorio1hora.isChecked, checkRecordatorio1dia.isChecked)

                // ✅ ESTO SE MANTIENE INTACTO - NOTIFICACIÓN AUTOMÁTICA ACTUAL
                viewModel.addEvent(event, requireContext())

                android.widget.Toast.makeText(
                    requireContext(),
                    "✅ ${getEventTypeDisplayName(eventType)} guardado para ${selectedDateForEvent.format(dateFormatter)} a las ${selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()

                updateCalendarHeader()
            } else {
                eventTitleEditText.error = "Ingresa un título para el evento"
            }
        }

        dialog.show()
        eventTitleEditText.requestFocus()
    }

    private fun getEventTypeDisplayName(eventType: String): String {
        return when (eventType) {
            EventType.ULTRASOUND.name -> "Ecografía"
            EventType.MEDICAL_APPOINTMENT.name -> "Cita Médica"
            EventType.PERSONAL_MILESTONE.name -> "Hito Personal"
            else -> "Evento"
        }
    }

    override fun onResume() {
        super.onResume()
        println("🔄 CalendarFragment - onResume() - Actualizando calendario")
        generateCalendarGrid()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}