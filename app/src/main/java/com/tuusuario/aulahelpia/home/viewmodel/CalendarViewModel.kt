package com.tuusuario.aulahelpia.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuusuario.aulahelpia.home.data.EventDao
import com.tuusuario.aulahelpia.home.data.ModuleType
import com.tuusuario.aulahelpia.home.data.PlanItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.tuusuario.aulahelpia.home.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import com.tuusuario.aulahelpia.home.data.HorarioItem
import com.tuusuario.aulahelpia.home.data.HorarioDao
import android.app.Application
import com.tuusuario.aulahelpia.home.data.HorarioStats

class CalendarViewModel(private val eventDao: EventDao, private val application: Application) : ViewModel() {

    private val horarioDao: HorarioDao = AppDatabase.getDatabase(application).horarioDao()

    // PRIMERO: Los formatters (deben estar antes de cualquier cosa que los use)
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.US)

    // DESPUÉS: Los StateFlows que usan los formatters
    private val _events = MutableStateFlow<List<PlanItem>>(emptyList())
    val events: StateFlow<List<PlanItem>> = _events.asStateFlow()

    private val _selectedDateString = MutableStateFlow(getTodayString())
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    private val _currentFilter = MutableStateFlow<ModuleType?>(null)
    val currentFilter: StateFlow<ModuleType?> = _currentFilter.asStateFlow()

    private val _currentCategoryFilter = MutableStateFlow<String?>(null)
    val currentCategoryFilter: StateFlow<String?> = _currentCategoryFilter.asStateFlow()

    // 🆕 FILTRADO POR BÚSQUEDA
    private val _searchFilter = MutableStateFlow<String?>(null)
    val searchFilter: StateFlow<String?> = _searchFilter.asStateFlow()

    fun setSearchFilter(query: String?) {
        _searchFilter.value = query?.trim()?.takeIf { it.isNotEmpty() }
    }

    // 🆕 MÉTODO ACTUALIZADO PARA EVENTOS FILTRADOS (usa String)
    fun getFilteredEventsForDate(dateString: String): List<PlanItem> {
        // ✅ FILTRAR: Solo tareas ACTIVAS o REPROGRAMADAS (excluir COMPLETADO y VENCIDO)
        var events = _events.value.filter {
            it.date == dateString &&
                    it.taskState != "COMPLETADO" &&
                    it.taskState != "VENCIDO"
        }

        // Aplicar filtro de categoría
        if (_currentCategoryFilter.value != null) {
            events = events.filter { it.category == _currentCategoryFilter.value }
        }

        // Aplicar filtro de búsqueda
        if (_searchFilter.value != null) {
            val query = _searchFilter.value!!.lowercase()
            events = events.filter {
                it.title.lowercase().contains(query) ||
                        it.description.lowercase().contains(query)
            }
        }

        return events
    }

    init {
        loadAllEvents()
    }

    // Helper para obtener fecha actual como String
    private fun getTodayString(): String {
        return isoFormatter.format(Calendar.getInstance().time)
    }

    // Helper para obtener fecha hace 2 años
    private fun getTwoYearsAgoString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -2)
        return isoFormatter.format(calendar.time)
    }

    // Helper para obtener fecha en 2 años
    private fun getTwoYearsLaterString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, 2)
        return isoFormatter.format(calendar.time)
    }

    // Helper para obtener mes actual
    private fun getCurrentMonthString(): String {
        return monthFormatter.format(Calendar.getInstance().time)
    }

    internal fun loadAllEvents() {
        viewModelScope.launch {
            eventDao.getEventsForMonth(
                startDate = getTwoYearsAgoString(),
                endDate = getTwoYearsLaterString()
            ).collect { allEvents ->
                _events.value = allEvents
                println("📅 CALENDAR VIEWMODEL - Eventos cargados: ${allEvents.size}")

                // Debug detallado
                if (allEvents.isEmpty()) {
                    println("⚠️ CALENDAR VIEWMODEL - Base de datos VACÍA")
                } else {
                    allEvents.take(3).forEachIndexed { index, event ->
                        println("📊 Evento $index: ${event.title} - ${event.date} - ${event.category}")
                    }
                }
            }
        }
    }

    fun addEvent(
        title: String,
        description: String,
        date: String,  // Cambiado a String
        time: String = "09:00",  // Cambiado a String
        moduleType: ModuleType = ModuleType.TASK,
        category: String = "general",
        priority: Int = 1
    ) {
        viewModelScope.launch {
            val newEvent = PlanItem(
                title = title,
                description = description,
                date = date,
                time = time,
                moduleType = moduleType,
                category = category,
                priority = priority,
                isCompleted = false,
                duration = 0
            )
            eventDao.insertEvent(newEvent)
        }
    }

    fun deleteEvent(event: PlanItem) {
        viewModelScope.launch {
            eventDao.deleteEvent(event)
        }
    }

    fun getEventsForDate(dateString: String): List<PlanItem> {
        return _events.value.filter { it.date == dateString }
    }

    fun getEventsForDateByModule(dateString: String, moduleType: ModuleType): List<PlanItem> {
        return _events.value.filter {
            it.date == dateString && it.moduleType == moduleType
        }
    }

    fun setSelectedDate(dateString: String) {
        _selectedDateString.value = dateString
    }

    fun setFilter(moduleType: ModuleType?) {
        _currentFilter.value = moduleType
    }

    fun setFilterByCategory(category: String?) {
        _currentCategoryFilter.value = category
        println("🎛️ FILTRO - Categoría: $category")
    }

    fun getFilteredEvents(): List<PlanItem> {
        var filteredEvents = if (_currentFilter.value != null) {
            _events.value.filter { it.moduleType == _currentFilter.value }
        } else {
            _events.value
        }

        // Aplicar filtro de categoría
        if (_currentCategoryFilter.value != null) {
            filteredEvents = filteredEvents.filter { it.category == _currentCategoryFilter.value }
        }

        // Aplicar filtro de búsqueda
        if (_searchFilter.value != null) {
            val query = _searchFilter.value!!.lowercase()
            filteredEvents = filteredEvents.filter {
                it.title.lowercase().contains(query) ||
                        it.description.lowercase().contains(query)
            }
        }

        return filteredEvents
    }

    fun getEventsCountForDate(dateString: String): Int {
        return getEventsForDate(dateString).size
    }

    fun getModuleEventsCountForDate(dateString: String, moduleType: ModuleType): Int {
        return getEventsForDateByModule(dateString, moduleType).size
    }

    fun getUpcomingEvents(limit: Int = 5): List<PlanItem> {
        val todayString = getTodayString()
        val calendar = Calendar.getInstance()
        val currentTimeString = timeFormatter.format(calendar.time)

        return _events.value
            .filter { event ->
                // Si la fecha es mayor que hoy → SÍ
                // Si la fecha es igual a hoy y la hora es mayor que ahora → SÍ
                // De lo contrario → NO
                event.date > todayString ||
                        (event.date == todayString && event.time > currentTimeString)
            }
            .sortedBy { it.date + it.time }
            .take(limit)
    }

    fun getMonthlyStats(): Map<ModuleType, Int> {
        val currentMonthString = getCurrentMonthString()
        return _events.value
            .filter { it.date.startsWith(currentMonthString) }
            .groupBy { it.moduleType }
            .mapValues { it.value.size }
    }
    fun guardarHorario(item: HorarioItem) {
        viewModelScope.launch {
            horarioDao.insertHorario(item)
        }
    }
    suspend fun existeClaseEnDiaYHora(dia: String, horaInicio: String): Boolean {
        return horarioDao.contarClases(dia, horaInicio) > 0
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
    suspend fun existeClaseEnRango(dia: String, horaInicio: String, horaFin: String): Boolean {
        val clases = horarioDao.getClasesPorDia(dia)

        println("🔍 VALIDACIÓN - Día: $dia")
        println("🔍 Nueva clase: $horaInicio - $horaFin")
        println("🔍 Clases existentes: ${clases.size}")

        val inicioMin = horaInicio.toMinutes()
        val finMin = horaFin.toMinutes()

        for (clase in clases) {
            val claseInicio = clase.horaInicio.toMinutes()
            val claseFin = clase.horaFin.toMinutes()

            println("🔍 Comparando con: ${clase.materia} (${clase.horaInicio} - ${clase.horaFin})")
            println("   Nueva: $inicioMin - $finMin, Existente: $claseInicio - $claseFin")

            if (inicioMin < claseFin && finMin > claseInicio) {
                println("🚨 SUPERPOSICIÓN DETECTADA!")
                return true
            }
        }
        println("✅ No hay superposición")
        return false
    }
    fun refreshEvents() {
        loadAllEvents()
    }
    fun getAllHorario(): Flow<List<HorarioItem>> {
        return horarioDao.getAllHorario()
    }
    fun eliminarHorario(item: HorarioItem) {
        viewModelScope.launch {
            horarioDao.deleteHorario(item)
        }
    }
    private fun String.toMinutes(): Int {
        val partes = this.split(":")
        val hora = partes.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val minuto = partes.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return hora * 60 + minuto
    }
        // 📊 OBTENER ESTADÍSTICAS DEL HORARIO
    fun getHorarioStats(): HorarioStats {
        val horario = horarioDao.getAllHorarioList()

        if (horario.isEmpty()) {
            return HorarioStats(
                totalClases = 0,
                materiasFrecuentes = emptyMap(),
                clasesCompletadas = 0,
                progresoPorcentaje = 0
            )
        }

        val totalClases = horario.size

        // Contar frecuencia de materias
        val frecuenciaMaterias = horario
            .groupBy { it.materia }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .toMap()

        val clasesCompletadas = totalClases

        val progreso = if (totalClases > 0) {
            (clasesCompletadas * 100 / totalClases)
        } else {
            0
        }

        return HorarioStats(
            totalClases = totalClases,
            materiasFrecuentes = frecuenciaMaterias,
            clasesCompletadas = clasesCompletadas,
            progresoPorcentaje = progreso
        )
    }
}