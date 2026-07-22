package com.tuusuario.aulahelpia.home.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HorarioDao {
    @Query("SELECT * FROM horario ORDER BY CASE dia " +
            "WHEN 'Lunes' THEN 1 " +
            "WHEN 'Martes' THEN 2 " +
            "WHEN 'Miércoles' THEN 3 " +
            "WHEN 'Jueves' THEN 4 " +
            "WHEN 'Viernes' THEN 5 " +
            "WHEN 'Sábado' THEN 6 " +
            "WHEN 'Domingo' THEN 7 END, horaInicio ASC")
    fun getAllHorario(): Flow<List<HorarioItem>>

    @Query("SELECT * FROM horario WHERE dia = :dia ORDER BY horaInicio ASC")
    fun getHorarioPorDia(dia: String): Flow<List<HorarioItem>>

    @Insert
    suspend fun insertHorario(item: HorarioItem)

    @Update
    suspend fun updateHorario(item: HorarioItem)

    @Delete
    suspend fun deleteHorario(item: HorarioItem)

    @Query("DELETE FROM horario")
    suspend fun deleteAllHorario()

    @Query("SELECT COUNT(*) FROM horario WHERE dia = :dia AND horaInicio = :horaInicio")
    suspend fun contarClases(dia: String, horaInicio: String): Int

    @Query("""
    SELECT COUNT(*) FROM horario 
    WHERE dia = :dia 
    AND (
        (horaInicio >= :horaInicio AND horaInicio < :horaFin) OR
        (horaFin > :horaInicio AND horaFin <= :horaFin) OR
        (horaInicio <= :horaInicio AND horaFin >= :horaFin)
    )
""")
    suspend fun contarClasesEnRango(dia: String, horaInicio: String, horaFin: String): Int

    @Query("SELECT * FROM horario WHERE dia = :dia")
    suspend fun getClasesPorDia(dia: String): List<HorarioItem>

    @Query("SELECT * FROM horario")
    fun getAllHorarioList(): List<HorarioItem>
}