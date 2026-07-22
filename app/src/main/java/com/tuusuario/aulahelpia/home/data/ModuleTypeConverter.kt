package com.tuusuario.aulahelpia.home.data

import androidx.room.TypeConverter

class ModuleTypeConverter {

    @TypeConverter
    fun fromModuleType(moduleType: ModuleType): String {
        return moduleType.name
    }

    @TypeConverter
    fun toModuleType(moduleTypeString: String): ModuleType {
        return try {
            ModuleType.valueOf(moduleTypeString)
        } catch (e: IllegalArgumentException) {
            ModuleType.TASK // Valor por defecto si hay error
        }
    }
}