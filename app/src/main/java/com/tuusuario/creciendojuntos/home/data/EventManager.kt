package com.tuusuario.creciendojuntos.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EventManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("pregnancy_events", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEvent(event: Event) {
        val allEvents = getEvents().toMutableList()

        // 🆕 CORREGIDO: Si el evento ya existe (mismo ID), reemplazarlo
        val existingIndex = allEvents.indexOfFirst { it.id == event.id }
        if (existingIndex != -1) {
            allEvents[existingIndex] = event
        } else {
            // 🆕 CORREGIDO: Si es nuevo evento y tiene ID = 0, asignar ID único
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

    fun getEvents(): List<Event> {
        val eventsJson = sharedPreferences.getString("events", "[]") ?: "[]"
        val type = object : TypeToken<List<Event>>() {}.type
        return gson.fromJson(eventsJson, type) ?: emptyList()
    }

    fun getEventsForDate(date: java.time.LocalDate): List<Event> {
        return getEvents().filter { it.date == date.toString() }
    }

    fun getEventsForMonth(year: Int, month: Int): List<Event> {
        return getEvents().filter { event ->
            val eventDate = java.time.LocalDate.parse(event.date)
            eventDate.year == year && eventDate.monthValue == month
        }
    }
    // 🆕 MÉTODO PARA ELIMINAR EVENTO
    fun deleteEvent(eventId: Long) {
        val allEvents = getEvents().toMutableList()
        allEvents.removeAll { it.id == eventId }
        val eventsJson = gson.toJson(allEvents)
        sharedPreferences.edit().putString("events", eventsJson).apply()
    }
}