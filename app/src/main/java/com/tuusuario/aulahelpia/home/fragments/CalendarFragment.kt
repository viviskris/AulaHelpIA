package com.tuusuario.aulahelpia.home.fragments

import android.os.Bundle
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.databinding.FragmentCalendarBinding
import com.tuusuario.aulahelpia.home.viewmodel.CalendarViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.tuusuario.aulahelpia.home.viewmodel.ViewModelFactory
import androidx.appcompat.widget.SearchView
import com.google.android.material.chip.Chip
import com.tuusuario.aulahelpia.home.utils.MotivationalMessages
import androidx.core.content.ContextCompat
import android.graphics.Color
import com.tuusuario.aulahelpia.home.utils.MateriasUtils
import com.tuusuario.aulahelpia.home.adapters.HorarioSemanalAdapter
import android.widget.Toast
import android.app.Application
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AlertDialog
import com.tuusuario.aulahelpia.home.data.HorarioItem
import kotlinx.coroutines.runBlocking

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels {
        ViewModelFactory(requireContext(), requireContext().applicationContext as Application)
    }
    private lateinit var horarioAdapter: HorarioSemanalAdapter
    private val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")

    private var currentMonthCalendar: Calendar = Calendar.getInstance()
    private var isWeeklyView = false
    private var currentWeekStartCalendar: Calendar = getStartOfWeek(Calendar.getInstance())

    // Formatters
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    private val dayMonthFormatter = SimpleDateFormat("dd MMM", Locale("es", "ES"))
    private val fullDateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

        println("🎯 CALENDAR FRAGMENT - INICIANDO...")

        setupAdMob()
        setupMotivationalMessage()
        setupClickListeners()
        setupFilterChips()
        setupSearchView()
        setupObservers()
        setupCalendar()
        setupHorario()
        updateSummaryAndUpcomingEvents()
        viewModel.refreshEvents()

        println("✅ CALENDAR FRAGMENT - INICIALIZACIÓN COMPLETA")
    }

    private fun setupAdMob() {
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewCalendar.loadAd(adRequest)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.events.collect { events ->
                if (!isAdded || view == null) {
                    println("⚠️ CALENDAR - Vista no disponible, ignorando actualización")
                    return@collect
                }

                println("🔔 CALENDAR - Eventos actualizados: ${events.size} eventos")
                generateCalendarGrid()
                updateSummaryAndUpcomingEvents()
            }
        }
    }

    private fun setupCalendar() {
        updateCalendarHeader()
        generateCalendarGrid()
    }
    private fun setupHorario() {
        val recyclerView = binding.rvHorarioSemanal
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 6)
        horarioAdapter = HorarioSemanalAdapter(emptyMap()) { clase ->
            mostrarDialogoEliminarClase(clase)
        }
        recyclerView.adapter = horarioAdapter
        cargarHorario()
    }

    private fun updateCalendarHeader() {
        binding.calendarTitle.text = monthYearFormatter.format(currentMonthCalendar.time)
    }

    private fun generateCalendarGrid() {
        if (!isAdded || _binding == null) {
            println("⚠️ CALENDAR - Vista no disponible, ignorando generateCalendarGrid")
            return
        }

        if (isWeeklyView) {
            generateWeeklyGrid()
        } else {
            generateMonthlyGrid()
        }
    }

    private fun generateMonthlyGrid() {
        if (!isAdded || _binding == null) {
            println("⚠️ CALENDAR - Vista no disponible, ignorando generateMonthlyGrid")
            return
        }

        println("🔄 GENERANDO CALENDARIO MENSUAL")
        println("📅 Mes actual: ${monthYearFormatter.format(currentMonthCalendar.time)}")

        val daysOfWeek = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        val weekDaysAdapter = object : BaseAdapter() {
            override fun getCount(): Int = daysOfWeek.size
            override fun getItem(position: Int): Any = daysOfWeek[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_week_day_header, parent, false)

                val textView = view.findViewById<TextView>(R.id.tvWeekDay)
                textView.text = daysOfWeek[position]

                return view
            }
        }
        binding.gvWeekDays.adapter = weekDaysAdapter

        val calendar = currentMonthCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val startOffset = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 0
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
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
                return createDayView(calendarDays[position], convertView, parent, isWeeklyView = false, position - startOffset + 1)
            }
        }

        binding.gvCalendarDays.adapter = daysAdapter
    }

    private fun generateWeeklyGrid() {
        if (!isAdded || _binding == null) {
            println("⚠️ CALENDAR - Vista no disponible, ignorando generateWeeklyGrid")
            return
        }

        println("🔄 GENERANDO CALENDARIO SEMANAL")

        val daysOfWeek = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        val weekDaysAdapter = object : BaseAdapter() {
            override fun getCount(): Int = daysOfWeek.size
            override fun getItem(position: Int): Any = daysOfWeek[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_week_day_header, parent, false)

                val textView = view.findViewById<TextView>(R.id.tvWeekDay)
                textView.text = daysOfWeek[position]

                return view
            }
        }

        binding.gvWeekDays.adapter = weekDaysAdapter

        val daysAdapter = object : BaseAdapter() {
            override fun getCount(): Int = 7
            override fun getItem(position: Int): Any = position
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val dayCalendar = currentWeekStartCalendar.clone() as Calendar
                dayCalendar.add(Calendar.DAY_OF_MONTH, position)

                val dayOfMonth = dayCalendar.get(Calendar.DAY_OF_MONTH)
                return createDayView(dayOfMonth.toString(), convertView, parent, isWeeklyView = true, dayOfMonth = dayOfMonth, dateCalendar = dayCalendar)
            }
        }

        binding.gvCalendarDays.adapter = daysAdapter
    }

    private fun createDayView(
        dayText: String,
        convertView: View?,
        parent: ViewGroup,
        isWeeklyView: Boolean,
        dayOfMonth: Int = -1,
        dateCalendar: Calendar? = null
    ): View {
        val layoutRes = if (isWeeklyView) R.layout.item_calendar_week_day else R.layout.item_calendar_day
        val dayView = convertView ?: LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)

        val dayTextView = dayView.findViewById<TextView>(R.id.tvDayNumber)
        dayTextView.text = dayText

        if (dayText.isNotEmpty() && dateCalendar != null) {
            if (isWeeklyView) {
                setupWeeklyDayView(dayView, dateCalendar)
            }

            highlightDate(dayView, dateCalendar)

            dayView.setOnClickListener {
                onDaySelected(dateCalendar)
            }
        } else if (dayText.isNotEmpty() && dayOfMonth > 0) {
            val dateCalendar = currentMonthCalendar.clone() as Calendar
            dateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            highlightDate(dayView, dateCalendar)
            dayView.setOnClickListener {
                onDaySelected(dateCalendar)
            }
        } else {
            dayView.setOnClickListener(null)
            dayTextView.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }

        return dayView
    }

    private fun setupWeeklyDayView(dayView: View, dateCalendar: Calendar) {
        val eventsCountText = dayView.findViewById<TextView?>(R.id.tvEventsCount)

        if (eventsCountText != null) {
            val dateString = isoFormatter.format(dateCalendar.time)
            val events = viewModel.getFilteredEventsForDate(dateString)

            if (events.isNotEmpty()) {
                eventsCountText.text = if (events.size > 99) "99+" else events.size.toString()
                eventsCountText.visibility = View.VISIBLE
            } else {
                eventsCountText.visibility = View.GONE
            }
        }
    }

    private fun highlightDate(dayView: View, dateCalendar: Calendar) {
        val dateString = isoFormatter.format(dateCalendar.time)
        val events = viewModel.getFilteredEventsForDate(dateString)
        val dayTextView = dayView.findViewById<TextView>(R.id.tvDayNumber)

        val today = Calendar.getInstance()
        val isToday = isSameDay(dateCalendar, today)

        when {
            isToday -> {
                dayView.setBackgroundResource(R.drawable.circle_today)
                dayTextView.setTextColor(requireContext().getColor(android.R.color.black))
            }
            events.isNotEmpty() -> {
                val firstEvent = events.first()
                val colorRes = firstEvent.getCategoryColorRes()
                dayView.setBackgroundResource(colorRes)

                val colorValue = requireContext().getColor(colorRes)
                val isColorClaro = esColorClaro(colorValue)

                dayTextView.setTextColor(
                    if (isColorClaro) {
                        requireContext().getColor(android.R.color.black)
                    } else {
                        requireContext().getColor(android.R.color.white)
                    }
                )
            }
            else -> {
                dayView.setBackgroundResource(0)
                dayTextView.setTextColor(requireContext().getColor(android.R.color.black))
            }
        }
    }

    private fun esColorClaro(color: Int): Boolean {
        val rojo = Color.red(color)
        val verde = Color.green(color)
        val azul = Color.blue(color)

        val luminosidad = (0.299 * rojo + 0.587 * verde + 0.114 * azul) / 255
        return luminosidad > 0.5
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun getStartOfWeek(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> cal.add(Calendar.DAY_OF_MONTH, -6)
            Calendar.MONDAY -> {} // Ya es lunes
            else -> {
                var daysToMonday = Calendar.MONDAY - cal.get(Calendar.DAY_OF_WEEK)  // ← CAMBIAR a var
                if (daysToMonday > 0) daysToMonday -= 7  // ← Ahora funciona
                cal.add(Calendar.DAY_OF_MONTH, daysToMonday)
            }
        }
        return cal
    }

    private fun setupClickListeners() {
        binding.prevMonthButton.setOnClickListener {
            if (isWeeklyView) {
                currentWeekStartCalendar.add(Calendar.WEEK_OF_YEAR, -1)
                binding.calendarTitle.text = getWeekRangeText()
            } else {
                currentMonthCalendar.add(Calendar.MONTH, -1)
                updateCalendarHeader()
            }
            generateCalendarGrid()
            viewModel.refreshEvents()
        }
        binding.btnEditHorario.setOnClickListener {
            mostrarDialogoAgregarHorario()
        }

        binding.nextMonthButton.setOnClickListener {
            if (isWeeklyView) {
                currentWeekStartCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                binding.calendarTitle.text = getWeekRangeText()
            } else {
                currentMonthCalendar.add(Calendar.MONTH, 1)
                updateCalendarHeader()
            }
            generateCalendarGrid()
            viewModel.refreshEvents()
        }

        setupToggleViewButton()
    }

    private fun setupToggleViewButton() {
        binding.btnToggleView.setOnClickListener {
            isWeeklyView = !isWeeklyView

            if (isWeeklyView) {
                binding.btnToggleView.text = "Vista Mes"
                binding.calendarTitle.text = getWeekRangeText()
                binding.gvCalendarDays.layoutParams.height = 120.dpToPx()
                // Asegurar que currentWeekStartCalendar sea el inicio de la semana actual
                currentWeekStartCalendar = getStartOfWeek(Calendar.getInstance())
            } else {
                binding.btnToggleView.text = "Vista Semana"
                updateCalendarHeader()
                binding.gvCalendarDays.layoutParams.height = 270.dpToPx()
            }

            generateCalendarGrid()
        }
    }

    private fun getWeekRangeText(): String {
        val start = currentWeekStartCalendar.time
        val endCal = currentWeekStartCalendar.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 6)
        val end = endCal.time

        return "${dayMonthFormatter.format(start)} - ${dayMonthFormatter.format(end)}"
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.setSearchFilter(null)
                    generateCalendarGrid()
                } else {
                    performSearch(newText)
                }
                return true
            }
        })
    }

    private fun performSearch(query: String?) {
        viewModel.setSearchFilter(query)
        generateCalendarGrid()
        println("🔍 BÚSQUEDA - Término: $query")
    }

    private fun setupFilterChips() {
        val chipGroup = binding.chipGroupFiltros
        chipGroup.removeAllViews()

        val materias = getMateriasGuardadas()

        // Chip "Todas"
        val chipTodos = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "📚 Todas"
            isChecked = true
            setChipBackgroundColorResource(R.color.purple_neon)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setChipStrokeColorResource(R.color.purple_neon)
        }
        chipGroup.addView(chipTodos)

        // Chips por materia
        for ((index, materia) in materias.withIndex()) {
            val emoji = MateriasUtils.getEmojiForMateria(materia)
            val colorRes = if (materia in MateriasUtils.materiasFijas.map { it.first }) {
                MateriasUtils.getColorRes(materia)
            } else {
                MateriasUtils.getColorForAdicionalPorNombre(materia)
            }

            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = "$emoji $materia"
                setChipBackgroundColorResource(colorRes)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                setChipStrokeColorResource(R.color.purple_neon)
            }
            chipGroup.addView(chip)
        }

        // Listener para los chips
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener {
                for (j in 0 until chipGroup.childCount) {
                    (chipGroup.getChildAt(j) as Chip).isChecked = false
                }
                chip.isChecked = true

                val texto = chip.text.toString()
                when {
                    texto == "📚 Todas" -> {
                        viewModel.setFilterByCategory(null)
                        println("🔍 Filtro: Todas las materias")
                    }
                    else -> {
                        val materiaNombre = texto.substringAfter(" ").trim()
                        viewModel.setFilterByCategory(materiaNombre)
                        println("🔍 Filtro por materia: $materiaNombre")
                    }
                }
                generateCalendarGrid()
            }
        }

        chipTodos.isChecked = true
        println("✅ CHIPS DE MATERIAS CARGADOS: ${materias.size + 1} chips")
    }
    // 📚 OBTENER MATERIAS GUARDADAS (desde SharedPreferences)
    private fun getMateriasGuardadas(): List<String> {
        return MateriasUtils.getMateriasGuardadas(requireContext())
    }

    private fun onDaySelected(dateCalendar: Calendar) {
        val dateString = isoFormatter.format(dateCalendar.time)
        val events = viewModel.getEventsForDate(dateString)
        val formattedDate = fullDateFormatter.format(dateCalendar.time)

        if (events.isNotEmpty()) {
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val eventDetails = events.joinToString("\n\n") { event ->
                val hora = if (event.time.isNotEmpty()) " - ${event.time}" else ""
                "• ${event.title}$hora" +
                        if (event.description.isNotEmpty()) "\n  ${event.description}" else ""
            }

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Eventos para $formattedDate")
                .setMessage(eventDetails)
                .setPositiveButton("Cerrar", null)
                .setNegativeButton("Eliminar Evento") { _, _ ->
                    showDeleteEventDialog(events, formattedDate)
                }
                .show()
        } else {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(formattedDate)
                .setMessage("No hay eventos para este día")
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun showDeleteEventDialog(events: List<com.tuusuario.aulahelpia.home.data.PlanItem>, fecha: String) {
        if (events.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No hay eventos para eliminar", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val items = arrayOfNulls<CharSequence>(events.size)
        events.forEachIndexed { index, event ->
            items[index] = "${event.title} - ${event.time}"
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Evento de $fecha")
            .setItems(items) { dialog, which ->
                val eventoAEliminar = events[which]
                confirmarEliminacion(eventoAEliminar)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacion(evento: com.tuusuario.aulahelpia.home.data.PlanItem) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Evento")
            .setMessage("¿Estás segura de que quieres eliminar '${evento.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteEvent(evento)
                android.widget.Toast.makeText(requireContext(), "✅ Evento eliminado", android.widget.Toast.LENGTH_SHORT).show()
                generateCalendarGrid()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateSummaryAndUpcomingEvents() {
        if (!isAdded || _binding == null) {
            println("⚠️ CALENDAR - Vista no disponible, ignorando updateSummaryAndUpcomingEvents")
            return
        }

        // Obtener mes actual en formato "yyyy-MM"
        val currentMonthString = SimpleDateFormat("yyyy-MM", Locale.US).format(currentMonthCalendar.time)

        val eventsThisMonth = viewModel.getFilteredEvents().count { event ->
            event.date.startsWith(currentMonthString)
        }

        binding.monthSummaryText.text =
            if (eventsThisMonth > 0)
                "📈 Este mes: $eventsThisMonth planes programados"
            else
                "📈 Este mes no hay planes aún"

        val upcomingEvents = viewModel.getUpcomingEvents(1)
        binding.upcomingEventsText.text = if (upcomingEvents.isNotEmpty()) {
            val nextEvent = upcomingEvents.first()
            val formattedDate = formatDateForDisplay(nextEvent.date)
            val time = nextEvent.time
            "🎯 Próximo: ${nextEvent.title} ($formattedDate $time)"
        } else {
            "🎯 No hay eventos próximos"
        }
    }

    private fun formatDateForDisplay(dateString: String): String {
        try {
            val date = isoFormatter.parse(dateString)
            val today = Calendar.getInstance()
            val eventDate = Calendar.getInstance().apply { time = date }

            return when {
                isSameDay(eventDate, today) -> "Hoy"
                isSameDay(eventDate, Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }) -> "Mañana"
                isSameDay(eventDate, Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }) -> "Ayer"
                else -> {
                    val formatter = SimpleDateFormat("dd/MM", Locale("es", "ES"))
                    formatter.format(date)
                }
            }
        } catch (e: Exception) {
            return dateString
        }
    }

    private fun setupMotivationalMessage() {
        try {
            println("🔍 CALENDAR - Configurando mensaje con animación")

            val message = MotivationalMessages.getCalendarMessage(requireContext())
            val prefs = requireContext().getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
            val counter = prefs.getInt("calendar_counter", 0)

            binding.tvMotivationalCalendar?.text = message

            val colors = listOf(
                R.color.primary_pastel,
                R.color.cyan_bright,
                R.color.cyan_bright,
                R.color.task_pastel,
                R.color.study_pastel
            )

            val colorIndex = if (counter > 0) (counter - 1) % colors.size else 0
            val colorRes = colors[colorIndex]

            binding.tvMotivationalCalendar?.setBackgroundColor(
                ContextCompat.getColor(requireContext(), colorRes)
            )

            binding.tvMotivationalCalendar?.setTextColor(Color.WHITE)

            binding.tvMotivationalCalendar?.let {
                MotivationalMessages.Animations.applyRandom(it, counter)
            }

            println("✅ CALENDAR - Mensaje animado: $message")

        } catch (e: Exception) {
            println("⚠️ CALENDAR - Error: ${e.message}")
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        generateCalendarGrid()
        updateSummaryAndUpcomingEvents()
    }
    private fun cargarHorario() {
        lifecycleScope.launch {
            viewModel.getAllHorario().collect { horarioItems ->
                val horarioPorDia = diasSemana.associateWith { dia ->
                    horarioItems.filter { it.dia == dia }
                }
                horarioAdapter.actualizarHorario(horarioPorDia)

                val tieneHorario = horarioItems.isNotEmpty()
                binding.tvHorarioVacio.visibility = if (tieneHorario) View.GONE else View.VISIBLE
            }
        }
    }
    private fun mostrarDialogoAgregarHorario() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_horario, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Referencias a los campos
        val actDia = dialogView.findViewById<AutoCompleteTextView>(R.id.actDia)
        val etHoraInicio = dialogView.findViewById<TextInputEditText>(R.id.etHoraInicio)
        val etHoraFin = dialogView.findViewById<TextInputEditText>(R.id.etHoraFin)
        val actMateria = dialogView.findViewById<AutoCompleteTextView>(R.id.actMateria)
        val etProfesor = dialogView.findViewById<TextInputEditText>(R.id.etProfesor)
        val etAula = dialogView.findViewById<TextInputEditText>(R.id.etAula)
        // Formatear hora al perder el foco
        etHoraInicio.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val texto = etHoraInicio.text.toString().trim()
                if (texto.isNotEmpty()) {
                    etHoraInicio.setText(formatearHora(texto))
                }
            }
        }

        etHoraFin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val texto = etHoraFin.text.toString().trim()
                if (texto.isNotEmpty()) {
                    etHoraFin.setText(formatearHora(texto))
                }
            }
        }
        val btnGuardar = dialogView.findViewById<MaterialButton>(R.id.btnGuardarHorario)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btnCancelarHorario)

        // Configurar días
        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
        val diaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dias)
        actDia.setAdapter(diaAdapter)

        // Configurar materias
        val materias = MateriasUtils.getMateriasGuardadas(requireContext())
        val materiaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, materias)
        actMateria.setAdapter(materiaAdapter)

// Forzar que las materias se muestren al hacer clic
        actMateria.setOnClickListener {
            actMateria.showDropDown()
        }

        // Botón Guardar
        btnGuardar.setOnClickListener {
            val dia = actDia.text.toString()
            val horaInicio = formatearHora(etHoraInicio.text.toString())
            val horaFin = formatearHora(etHoraFin.text.toString())
            val materia = actMateria.text.toString()
            val profesor = etProfesor.text.toString()
            val aula = etAula.text.toString()

            if (dia.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty() || materia.isEmpty()) {
                Toast.makeText(requireContext(), "⚠️ Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar si ya existe una clase en ese día y hora
            try {
                val existe = try {
                    runBlocking {
                        viewModel.existeClaseEnRango(dia, horaInicio, horaFin)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false // Si hay error, asumir que no existe conflicto
                }
                if (existe) {
                    Toast.makeText(requireContext(), "⚠️ Horario ocupado en $dia de $horaInicio a $horaFin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Guardar en la base de datos
                val horarioItem = HorarioItem(
                    dia = dia,
                    horaInicio = horaInicio,
                    horaFin = horaFin,
                    materia = materia,
                    profesor = profesor,
                    aula = aula
                )

                viewModel.guardarHorario(horarioItem)
                Toast.makeText(requireContext(), "✅ Clase agregada", Toast.LENGTH_SHORT).show()
                cargarHorario()
                dialog.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "❌ Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón Cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun mostrarDialogoEliminarClase(clase: HorarioItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar clase")
            .setMessage("¿Eliminar ${clase.materia} del ${clase.dia} a las ${clase.horaInicio}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarHorario(clase)
                Toast.makeText(requireContext(), "✅ Clase eliminada", Toast.LENGTH_SHORT).show()
                cargarHorario()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun formatearHora(input: String): String {
        // Eliminar espacios y caracteres no numéricos
        val numeros = input.replace(Regex("[^0-9]"), "")

        return when {
            numeros.length == 3 -> {
                // Ej: "715" → "07:15"
                val hora = numeros.substring(0, 1)
                val minuto = numeros.substring(1, 3)
                "${hora.padStart(2, '0')}:$minuto"
            }
            numeros.length == 4 -> {
                // Ej: "0715" → "07:15"
                val hora = numeros.substring(0, 2)
                val minuto = numeros.substring(2, 4)
                "$hora:$minuto"
            }
            input.contains(":") -> {
                // Si ya tiene dos puntos, validar que sea HH:mm
                val partes = input.split(":")
                if (partes.size == 2) {
                    val hora = partes[0].padStart(2, '0')
                    val minuto = partes[1].padStart(2, '0')
                    "$hora:$minuto"
                } else {
                    input // Devolver original si no se puede formatear
                }
            }
            else -> input // Devolver original
        }
    }
    private fun normalizarHora(hora: String): String {
        val partes = hora.split(":")
        if (partes.size == 2) {
            val h = partes[0].padStart(2, '0')
            val m = partes[1].padStart(2, '0')
            return "$h:$m"
        }
        return "00:00"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}