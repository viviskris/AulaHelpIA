package com.tuusuario.aulahelpia.home.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class EventManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("aulahelpia_events", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEvent(event: PlanItem) {
        val allEvents = getEvents().toMutableList()

        // Si el evento ya existe (mismo ID), reemplazarlo
        val existingIndex = allEvents.indexOfFirst { it.id == event.id }
        if (existingIndex != -1) {
            allEvents[existingIndex] = event
        } else {
            // Si es nuevo evento y tiene ID = 0, asignar ID único
            val newEvent = if (event.id == 0L) {
                event.copy(id = System.currentTimeMillis())
            } else {
                event
            }
            allEvents.add(newEvent)
        }
        val eventsJson = gson.toJson(allEvents)
        sharedPreferences.edit().putString("events", eventsJson).apply()
    }

    fun getEvents(): List<PlanItem> {
        val eventsJson = sharedPreferences.getString("events", "[]") ?: "[]"
        val type = object : TypeToken<List<PlanItem>>() {}.type
        return gson.fromJson(eventsJson, type) ?: emptyList()
    }

    fun getEventsForDate(date: LocalDate): List<PlanItem> {
        return getEvents().filter { it.date == date.toString() }
    }

    fun getEventsForMonth(year: Int, month: Int): List<PlanItem> {
        return getEvents().filter { event ->
            val eventDate = LocalDate.parse(event.date)
            eventDate.year == year && eventDate.monthValue == month
        }
    }

    // MÉTODO PARA ELIMINAR EVENTO
    fun deleteEvent(eventId: Long) {
        val allEvents = getEvents().toMutableList()
        allEvents.removeAll { it.id == eventId }
        val eventsJson = gson.toJson(allEvents)
        sharedPreferences.edit().putString("events", eventsJson).apply()
    }

    // NUEVOS MÉTODOS PARA FILTRAR POR MÓDULO (usando ModuleType enum)
    fun getEventsByModule(moduleType: ModuleType): List<PlanItem> {
        return getEvents().filter { it.moduleType == moduleType }
    }

    fun getTasks(): List<PlanItem> {
        return getEventsByModule(ModuleType.TASK)
    }

    fun getExercises(): List<PlanItem> {
        return getEventsByModule(ModuleType.EXERCISE)
    }

    fun getNutritionItems(): List<PlanItem> {
        return getEventsByModule(ModuleType.NUTRITION)
    }

    fun getStudyItems(): List<PlanItem> {
        return getEventsByModule(ModuleType.STUDY)
    }

    fun getPersonalItems(): List<PlanItem> {
        return getEventsByModule(ModuleType.PERSONAL)
    }

    fun getImportantItems(): List<PlanItem> {
        return getEventsByModule(ModuleType.IMPORTANT)
    }

    // MÉTODO PARA OBTENER ESTADÍSTICAS
    fun getModuleStats(): Map<ModuleType, Int> {
        return getEvents()
            .groupBy { it.moduleType }
            .mapValues { it.value.size }
    }
}