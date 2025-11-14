package com.tuusuario.creciendojuntos.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.* // ✅ Este import incluye Date y Calendar
import java.text.SimpleDateFormat // ✅ Para formatear fechas si lo necesitas
import com.tuusuario.creciendojuntos.home.model.PregnancyProgress
import android.content.Context
import com.tuusuario.creciendojuntos.data.Event
import com.tuusuario.creciendojuntos.data.EventManager
import com.tuusuario.creciendojuntos.home.data.EventType
import com.tuusuario.creciendojuntos.home.utils.NotificationHelper

class PregnancyViewModel : ViewModel() {


    // LiveData para observar cambios
    private val _dueDate = MutableLiveData<LocalDate?>()
    val dueDate: LiveData<LocalDate?> = _dueDate

    private val _pregnancyProgress = MutableLiveData<PregnancyProgress>()
    val pregnancyProgress: LiveData<PregnancyProgress> = _pregnancyProgress

    private val _lastPeriodDate = MutableLiveData<LocalDate?>()
    val lastPeriodDate: LiveData<LocalDate?> = _lastPeriodDate

    // Eventos en memoria (para compatibilidad)
    private val _events = MutableLiveData<MutableList<CalendarEvent>>()
    val events: LiveData<MutableList<CalendarEvent>> = _events

    init {
        _events.value = mutableListOf()
    }

    // Configurar fecha de última menstruación
    fun setLastPeriodDate(lastPeriod: LocalDate) {
        _lastPeriodDate.value = lastPeriod
        calculateDueDateFromLastPeriod(lastPeriod)
    }

    // Configurar fecha probable de parto directamente
    fun setDueDate(date: LocalDate) {
        _dueDate.value = date
        calculatePregnancyProgress(date)
    }

    // Calcular Fecha Probable de Parto (FPP) desde última menstruación
    private fun calculateDueDateFromLastPeriod(lastPeriod: LocalDate) {
        val dueDate = lastPeriod.plusDays(280) // 40 semanas
        _dueDate.value = dueDate
        calculatePregnancyProgress(dueDate)
    }

    // Calcular progreso actual del embarazo
    fun calculatePregnancyProgress(dueDate: LocalDate) {
        val today = LocalDate.now()
        val startDate = dueDate.minusDays(280) // 40 semanas de embarazo

        val totalWeeks = ChronoUnit.WEEKS.between(startDate, today).toInt()
        val daysInCurrentWeek = ChronoUnit.DAYS.between(
            startDate.plusWeeks(totalWeeks.toLong()),
            today
        ).toInt()

        val weeksPregnant = totalWeeks.coerceIn(0, 40)
        val weeksRemaining = (40 - weeksPregnant).coerceAtLeast(0)

        val progress = PregnancyProgress(
            currentWeek = weeksPregnant,
            daysInCurrentWeek = daysInCurrentWeek.coerceIn(0, 6),
            weeksRemaining = weeksRemaining,
            babySize = getBabySizeForWeek(weeksPregnant),
            babyDevelopment = getBabyDevelopmentForWeek(weeksPregnant),
            progressPercentage = (weeksPregnant * 7 + daysInCurrentWeek).toFloat() / 280f * 100f
        )

        _pregnancyProgress.value = progress
    }

    // Tamaño del bebé según la semana
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

    // Desarrollo del bebé según la semana
    private fun getBabyDevelopmentForWeek(week: Int): String {
        return when (week) {
            in 1..4 -> "Se forman el corazón y el sistema nervioso"
            in 5..8 -> "Desarrollo de brazos, piernas y órganos principales"
            in 9..12 -> "Los dedos se separan, aparecen las uñas"
            in 13..16 -> "El bebé puede chuparse el dedo"
            in 17..20 -> "La mamá puede sentir los movimientos"
            in 21..24 -> "Desarrollo de huellas digitales"
            in 25..28 -> "Abre y cierra los ojos"
            in 29..32 -> "Aumento rápido de peso"
            in 33..36 -> "Los pulmones están casi maduros"
            in 37..40 -> "Posición lista para nacer"
            else -> "Desarrollo en proceso"
        }
    }

    // Formatear fecha en español
    fun formatDueDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return date.format(formatter)
    }

    // Clase para representar eventos
    data class CalendarEvent(
        val id: Long = System.currentTimeMillis(),
        val title: String,
        val description: String,
        val date: LocalDate,
        val time: LocalTime = LocalTime.of(9, 0), // 🆕 HORA POR DEFECTO: 9:00 AM
        val type: String = "PERSONAL"
    )

    // ===== MÉTODOS DE EVENTOS PERSISTENTES =====

    // Método para agregar eventos PERSISTENTES (CON CONTEXTO)
    fun addEvent(event: CalendarEvent, context: Context) {
        // 🆕 DEBUG: Ver qué hora tiene el evento antes de guardar
        println("🔍 DEBUG ANTES DE GUARDAR - Hora del evento: '${event.time}'")

        // Guardar en persistencia
        val manager = EventManager(context)
        val persistentEvent = Event(
            id = event.id,
            title = event.title,
            description = event.description,
            date = event.date.toString(),
            time = event.time.toString(),
            type = event.type
        )
        // 🆕 DEBUG: Ver qué hora se guarda en el Event persistente
        println("🔍 DEBUG GUARDANDO - Hora persistente: '${persistentEvent.time}'")
        manager.saveEvent(persistentEvent)

        // También mantener en memoria para compatibilidad
        val currentEvents = _events.value ?: mutableListOf()
        currentEvents.add(event)
        _events.value = currentEvents

        // 🆕 🚨 COMENTAR ESTAS 2 LÍNEAS - LOS RECORDATORIOS LOS MANEJA EL CALENDARFRAGMENT 🚨
        // val notificationHelper = NotificationHelper(context)
        // notificationHelper.scheduleEventReminder(event, 60) // 60 minutos antes

        // 🆕 AGREGAR ESTE LOG EN SU LUGAR
        println("✅ EVENTO GUARDADO - Los recordatorios los maneja el CalendarFragment")
    }

    // 🆕 MÉTODO PARA ELIMINAR EVENTOS
    fun deleteEvent(eventId: Long, context: Context) {
        val manager = EventManager(context)
        manager.deleteEvent(eventId)

        // También eliminar de memoria
        val currentEvents = _events.value ?: mutableListOf()
        _events.value = currentEvents.filter { it.id != eventId }.toMutableList() // 🆕 AGREGAR .toMutableList()
    }

    // Método para obtener eventos de una fecha específica (PERSISTENTES CON CONTEXTO)
    fun getEventsForDate(date: LocalDate, context: Context): List<CalendarEvent> {
        // Usar eventos persistentes
        val manager = EventManager(context)
        val persistentEvents = manager.getEventsForDate(date)

        // 🆕 DEBUG: Ver qué hora tienen los eventos al recuperarlos
        persistentEvents.forEach { persistentEvent ->
            println("🔍 DEBUG RECUPERANDO - Evento: '${persistentEvent.title}' - Hora: '${persistentEvent.time}'")
        }

        if (persistentEvents.isNotEmpty()) {
            return persistentEvents.map { persistentEvent ->
                CalendarEvent(
                    id = persistentEvent.id,
                    title = persistentEvent.title,
                    description = persistentEvent.description,
                    date = LocalDate.parse(persistentEvent.date),
                    time = if (persistentEvent.time.isNullOrEmpty()) {
                        LocalTime.of(9, 0) // Hora por defecto para eventos antiguos
                    } else {
                        LocalTime.parse(persistentEvent.time) // 🆕 AGREGAR ESTA LÍNEA
                    },
                    type = persistentEvent.type
                )
            }
        }

        // Fallback a eventos en memoria
        return _events.value?.filter { it.date == date } ?: emptyList()
    }

    // Método para obtener todos los eventos del mes (para el calendario)
    fun getEventsForMonth(year: Int, month: Int, context: Context): List<CalendarEvent> {
        val manager = EventManager(context)
        val persistentEvents = manager.getEventsForMonth(year, month)
        return persistentEvents.map { persistentEvent ->
            CalendarEvent(
                id = persistentEvent.id,
                title = persistentEvent.title,
                description = persistentEvent.description,
                date = LocalDate.parse(persistentEvent.date),
                time = if (persistentEvent.time.isNullOrEmpty()) {
                    LocalTime.of(9, 0)
                } else {
                    LocalTime.parse(persistentEvent.time) // 🆕 AGREGAR ESTA LÍNEA
                },
                type = persistentEvent.type
            )
        }
    }

    // ===== MÉTODOS PARA NUEVOS TIPOS DE EVENTOS =====

    fun addUltrasoundEvent(date: LocalDate, time: String, description: String, context: Context) {
        val event = CalendarEvent(
            title = "🔴 Ecografía: $description",
            description = description,
            date = date,
            time = LocalTime.parse(time),
            type = EventType.ULTRASOUND.name
        )
        addEvent(event, context)
    }

    fun addMedicalAppointment(date: LocalDate, time: String, description: String, context: Context) {
        val event = CalendarEvent(
            title = "🔵 Cita Médica: $description",
            description = description,
            date = date,
            time = LocalTime.parse(time),
            type = EventType.MEDICAL_APPOINTMENT.name
        )
        addEvent(event, context)
    }

    fun addPersonalMilestone(date: LocalDate, time: String, description: String, context: Context) {
        val event = CalendarEvent(
            title = "🟠 Hito Personal: $description",
            description = description,
            date = date,
            time = LocalTime.parse(time),
            type = EventType.PERSONAL_MILESTONE.name
        )
        addEvent(event, context)
    }

    // Método para obtener eventos por tipo
    fun getEventsByType(type: EventType, context: Context): List<CalendarEvent> {
        val manager = EventManager(context)

        // Obtener eventos de los próximos 2 años para cubrir todo el embarazo
        val currentYear = LocalDate.now().year
        val allEvents = mutableListOf<CalendarEvent>()

        // Buscar en el año actual y el próximo
        for (year in currentYear..currentYear + 1) {
            for (month in 1..12) {
                val monthlyEvents = manager.getEventsForMonth(year, month)
                allEvents.addAll(monthlyEvents.map { persistentEvent ->
                    CalendarEvent(
                        id = persistentEvent.id,
                        title = persistentEvent.title,
                        description = persistentEvent.description,
                        date = LocalDate.parse(persistentEvent.date),
                        time = if (persistentEvent.time.isNullOrEmpty()) {
                            LocalTime.of(9, 0)
                        } else {
                            LocalTime.parse(persistentEvent.time) // 🆕 AGREGAR ESTA LÍNEA
                        },
                        type = persistentEvent.type
                    )
                })
            }
        }

        // Filtrar por tipo
        return allEvents.filter { it.type == type.name }
    }

    // ===== MÉTODOS NUEVOS PARA EL PERFIL =====
    data class UserProfile(
        var userName: String = "Usuario CreciendoJuntos",
        var userEmail: String = "usuario@creciendojuntos.com",
        var fullName: String = "Nombre Completo",
        var lastUpdate: String = "Hoy"
    )

    private val _currentProfile = MutableLiveData<UserProfile>()
    val currentProfile: LiveData<UserProfile> get() = _currentProfile

    init {
        // Inicializar perfil en el init existente
        _currentProfile.value = UserProfile()
    }

    // 🆕 MÉTODOS ESPECÍFICOS PARA PERFIL (no conflicto con los existentes)
    fun updateProfileDueDate(date: LocalDate) {
        // Usa el método existente setDueDate() que ya funciona
        setDueDate(date)
        updateProfileLastUpdate()
    }

    fun updateProfileLastPeriod(date: LocalDate) {
        // Usa el método existente setLastPeriodDate() que ya funciona
        setLastPeriodDate(date)
        updateProfileLastUpdate()
    }

    // ✅ MÉTODOS NUEVOS PARA EL PERFIL:
    private fun updateProfileLastUpdate() {
        val currentProfile = _currentProfile.value ?: UserProfile()
        _currentProfile.value = currentProfile.copy(lastUpdate = "Ahora")
        println("🔄 VIEWMODEL - Perfil actualizado: Ahora")
    }

    fun saveProfileDueDateToPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        _dueDate.value?.let { dueDate ->
            sharedPref.edit().putString("due_date", dueDate.toString()).apply()
            println("💾 VIEWMODEL - Fecha de parto guardada: $dueDate")
        }
        _lastPeriodDate.value?.let { lastPeriod ->
            sharedPref.edit().putString("last_period_date", lastPeriod.toString()).apply()
            println("💾 VIEWMODEL - Última regla guardada: $lastPeriod")
        }
    }

    fun saveProfileToPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val profile = _currentProfile.value ?: UserProfile()

        sharedPref.edit().apply {
            putString("profile_name", profile.userName)
            putString("profile_email", profile.userEmail)
            putString("profile_fullname", profile.fullName)
            putString("profile_last_update", profile.lastUpdate)
            apply()
        }
    }

    fun loadProfileFromPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val profile = UserProfile(
            userName = sharedPref.getString("profile_name", "Usuario CreciendoJuntos") ?: "Usuario CreciendoJuntos",
            userEmail = sharedPref.getString("profile_email", "usuario@creciendojuntos.com") ?: "usuario@creciendojuntos.com",
            fullName = sharedPref.getString("profile_fullname", "Nombre Completo") ?: "Nombre Completo",
            lastUpdate = sharedPref.getString("profile_last_update", "Hoy") ?: "Hoy"
        )
        _currentProfile.value = profile
    }

    // MODIFICA el método updateProfile para guardar automáticamente:
    fun updateProfile(name: String, email: String, fullName: String, context: Context? = null) {
        val updatedProfile = UserProfile(
            userName = name,
            userEmail = email,
            fullName = fullName,
            lastUpdate = "Ahora"
        )
        _currentProfile.value = updatedProfile

        // Guardar en SharedPreferences si tenemos contexto
        context?.let {
            saveProfileToPreferences(it)
        }
    }
}