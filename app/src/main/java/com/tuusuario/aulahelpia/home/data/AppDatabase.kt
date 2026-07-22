package com.tuusuario.aulahelpia.home.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlanItem::class, HorarioItem::class], // 👈 Agregar HorarioItem
    version = 5, // 👈 Subir a 5
    exportSchema = false
)
@TypeConverters(ModuleTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun horarioDao(): HorarioDao

    companion object {
        // Migración de VERSIÓN 1 a 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plan_items ADD COLUMN taskState TEXT NOT NULL DEFAULT 'ACTIVO'")
                println("🔄 DATABASE - Migración 1→2 completada")
            }
        }

        // Migración de VERSIÓN 2 a 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plan_items ADD COLUMN notificationSoundUri TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plan_items ADD COLUMN notificationVibration INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE plan_items ADD COLUMN notificationLedColor TEXT")
                println("🔊 DATABASE - Migración 2→3 completada")
            }
        }

        // 🆕 Migración de VERSIÓN 3 a 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Sin cambios estructurales entre 3 y 4
                // Solo se aumenta la versión para mantener consistencia
                println("🔄 DATABASE - Migración 3→4 completada (sin cambios)")
            }
        }
        // 🆕 Migración de VERSIÓN 4 a 5 (crear tabla horario)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE horario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dia TEXT NOT NULL,
                horaInicio TEXT NOT NULL,
                horaFin TEXT NOT NULL,
                materia TEXT NOT NULL,
                profesor TEXT NOT NULL DEFAULT '',
                aula TEXT NOT NULL DEFAULT ''
            )
        """)
                println("🔄 DATABASE - Migración 4→5: Tabla horario creada")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aulahelpia_v3_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration() // 👈 TEMPORAL
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}