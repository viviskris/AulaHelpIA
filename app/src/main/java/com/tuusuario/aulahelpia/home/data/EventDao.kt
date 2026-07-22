package com.tuusuario.aulahelpia.home.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.Update

@Dao
interface EventDao {

    // ✅ Obtener planes por fecha
    @Query("SELECT * FROM plan_items WHERE date = :date ORDER BY time ASC")
    fun getEventsForDate(date: String): Flow<List<PlanItem>>

    // ✅ Obtener planes por rango de fechas (para calendario mensual)
    @Query("SELECT * FROM plan_items WHERE date BETWEEN :startDate AND :endDate ORDER BY date, time ASC")
    fun getEventsForMonth(startDate: String, endDate: String): Flow<List<PlanItem>>

    // ✅ NUEVAS QUERIES PARA LOS MÓDULOS - Usando ModuleType enum
    @Query("SELECT * FROM plan_items WHERE moduleType = :moduleType ORDER BY date, time ASC")
    fun getItemsByModule(moduleType: ModuleType): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE moduleType = :moduleType AND date = :date ORDER BY time ASC")
    fun getModuleItemsForDate(moduleType: ModuleType, date: String): Flow<List<PlanItem>>

    // ✅ Obtener planes por estado de completado
    @Query("SELECT * FROM plan_items WHERE isCompleted = :completed ORDER BY date, time ASC")
    fun getItemsByCompletion(completed: Boolean): Flow<List<PlanItem>>

    // ✅ Actualizar estado de completado
    @Query("UPDATE plan_items SET isCompleted = :completed WHERE id = :itemId")
    suspend fun updateCompletionStatus(itemId: Long, completed: Boolean)

    // ✅ Obtener estadísticas por módulo
    @Query("SELECT COUNT(*) FROM plan_items WHERE moduleType = :moduleType AND isCompleted = :completed")
    suspend fun getModuleItemCount(moduleType: ModuleType, completed: Boolean): Int

    // ✅ Obtener planes próximos (hoy y futuro)
    @Query("SELECT * FROM plan_items WHERE date >= :today ORDER BY date, time ASC LIMIT :limit")
    fun getUpcomingItems(today: String, limit: Int = 5): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items ORDER BY date, time ASC")
    fun getAllEvents(): Flow<List<PlanItem>>

    // 🆕 QUERIES POR ESTADO
    @Query("SELECT * FROM plan_items WHERE taskState = :state ORDER BY date, time ASC")
    fun getItemsByState(state: String): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE taskState IN (:states) ORDER BY date, time ASC")
    fun getItemsByMultipleStates(states: List<String>): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE date = :date AND taskState = :state ORDER BY time ASC")
    fun getItemsForDateByState(date: String, state: String): Flow<List<PlanItem>>

    @Query("UPDATE plan_items SET taskState = :newState WHERE id = :itemId")
    suspend fun updateTaskState(itemId: Long, newState: String)

    @Query("UPDATE plan_items SET taskState = :newState, isCompleted = :completed WHERE id = :itemId")
    suspend fun updateTaskStateAndCompletion(itemId: Long, newState: String, completed: Boolean)

    // 🆕 QUERY PARA ACTUALIZAR ESTADOS VENCIDOS AUTOMÁTICAMENTE
    @Query("UPDATE plan_items SET taskState = 'VENCIDO' WHERE date < :today AND taskState = 'ACTIVO'")
    suspend fun updateOverdueTasks(today: String)

    // ✅ OPERACIONES CRUD BÁSICAS
    @Insert
    suspend fun insertEvent(event: PlanItem)

    @Update
    suspend fun updateEvent(event: PlanItem)

    @Delete
    suspend fun deleteEvent(event: PlanItem)

    @Query("DELETE FROM plan_items WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    // ✅ Limpiar todos los planes (para gestión de datos)
    @Query("DELETE FROM plan_items")
    suspend fun deleteAllEvents()


}