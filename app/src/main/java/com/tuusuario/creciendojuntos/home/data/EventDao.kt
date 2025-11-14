package com.tuusuario.creciendojuntos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE date = :date")
    fun getEventsForDate(date: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE date BETWEEN :startDate AND :endDate")
    fun getEventsForMonth(startDate: String, endDate: String): Flow<List<Event>>

    @Insert
    suspend fun insertEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)
}