package com.tuusuario.aulahelpia.home.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuusuario.aulahelpia.home.data.EventDao
import com.tuusuario.aulahelpia.home.data.ModuleType
import com.tuusuario.aulahelpia.home.data.PlanItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(
    private val eventDao: EventDao,
    private val context: Context? = null
) : ViewModel() {

    // Estado del perfil de usuario
    private val _currentProfile = MutableStateFlow(UserProfile())
    val currentProfile: StateFlow<UserProfile> = _currentProfile.asStateFlow()

    // Estadísticas de uso
    private val _usageStats = MutableStateFlow(UsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    // Preferencias de la app
    private val _appPreferences = MutableStateFlow(AppPreferences())
    val appPreferences: StateFlow<AppPreferences> = _appPreferences.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val prefs: SharedPreferences? = context?.getSharedPreferences("aulahelpia_prefs", Context.MODE_PRIVATE)

    init {
        loadInitialData()
        observeUserData()
    }

    // ============ AGREGAR AQUÍ - INICIO DE LA SECCIÓN DE FOTO ============

    // Estado para la foto de perfil
    private val _profilePhotoUri = MutableStateFlow<Uri?>(null)
    val profilePhotoUri: StateFlow<Uri?> = _profilePhotoUri.asStateFlow()

    // Directorio para guardar fotos
    private lateinit var profilePhotosDir: File

    // Archivo de la foto actual
    private lateinit var currentPhotoFile: File

    // Inicializar después de tener el contexto
    private fun initPhotoStorage() {
        // Verificar que tenemos contexto
        if (context == null) {
            println("❌ ProfileViewModel - Context es null, no se puede inicializar foto")
            return
        }

        profilePhotosDir = File(context.filesDir, "profile_photos").apply {
            if (!exists()) mkdirs()
            println("✅ Directorio de fotos creado: $absolutePath")
        }
        currentPhotoFile = File(profilePhotosDir, "current_profile_photo.jpg")
        loadProfilePhoto()
    }

    private fun loadProfilePhoto() {
        viewModelScope.launch {
            if (::currentPhotoFile.isInitialized && currentPhotoFile.exists()) {
                _profilePhotoUri.value = Uri.fromFile(currentPhotoFile)
                println("✅ Foto de perfil cargada: ${currentPhotoFile.absolutePath}")
            } else {
                _profilePhotoUri.value = null
                println("ℹ️ No hay foto de perfil guardada")
            }
        }
    }
    fun saveProfilePhoto(bitmap: Bitmap): Boolean {
        return try {
            if (!::currentPhotoFile.isInitialized) {
                initPhotoStorage()
            }

            if (context == null) {
                println("❌ No se puede guardar foto: contexto es null")
                return false
            }

            FileOutputStream(currentPhotoFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            // 🆕 Forzar emisión
            val newUri = Uri.fromFile(currentPhotoFile)
            viewModelScope.launch {
                _profilePhotoUri.value = newUri
            }

            println("✅ Foto guardada exitosamente: ${currentPhotoFile.absolutePath}")
            true
        } catch (e: Exception) {
            println("❌ Error al guardar foto: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    fun saveProfilePhotoFromUri(uri: Uri): Boolean {
        return try {
            if (!::currentPhotoFile.isInitialized) {
                initPhotoStorage()
            }

            if (context == null) {
                println("❌ No se puede guardar foto desde URI: contexto es null")
                return false
            }

            // 1. Copiar la imagen al archivo
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(currentPhotoFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 2. 🆕 IMPORTANTE: Forzar una nueva emisión del StateFlow
            val newUri = Uri.fromFile(currentPhotoFile)
            println("🔄 Emitiendo nuevo URI: $newUri")

            // Usar viewModelScope para actualizar el StateFlow
            viewModelScope.launch {
                _profilePhotoUri.value = newUri
            }

            println("✅ Foto desde URI guardada: ${currentPhotoFile.absolutePath}")
            true
        } catch (e: Exception) {
            println("❌ Error al guardar foto desde URI: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun deleteProfilePhoto(): Boolean {
        return try {
            if (::currentPhotoFile.isInitialized && currentPhotoFile.exists()) {
                val deleted = currentPhotoFile.delete()
                if (deleted) {
                    _profilePhotoUri.value = null
                    println("✅ Foto de perfil eliminada")
                }
                deleted
            } else {
                println("ℹ️ No hay foto para eliminar")
                false
            }
        } catch (e: Exception) {
            println("❌ Error al eliminar foto: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun hasProfilePhoto(): Boolean {
        return ::currentPhotoFile.isInitialized && currentPhotoFile.exists()
    }

    // Llamar este método cuando se inicialice el ViewModel
    fun initializePhotoStorage() {
        if (!::profilePhotosDir.isInitialized) {
            initPhotoStorage()
        }
    }

    private fun loadInitialData() {
        // Cargar perfil desde SharedPreferences si existe
        loadProfileFromPreferences()

        // Cargar preferencias desde SharedPreferences
        loadPreferencesFromStorage()
    }

    private fun observeUserData() {
        viewModelScope.launch {
            combine(
                eventDao.getItemsByCompletion(true), // Completados
                eventDao.getItemsByCompletion(false) // Pendientes
            ) { completed, pending ->
                calculateUsageStats(completed, pending)
            }.collect { stats ->
                _usageStats.value = stats
            }
        }
    }

    // 🎯 ACTUALIZAR PERFIL CON PERSISTENCIA
    fun updateProfile(name: String, email: String, fullName: String) {
        val updatedProfile = UserProfile(
            userName = name.ifEmpty { "Usuario AulaHelpIA" },
            userEmail = email.ifEmpty { "usuario@aulahelpia.com" },
            fullName = fullName.ifEmpty { "Nombre Completo" },
            memberSince = getMemberSince(),
            lastUpdate = "Actualizado hoy"
        )
        _currentProfile.value = updatedProfile

        // Guardar en SharedPreferences
        saveProfileToPreferences(updatedProfile)
    }

    // 📊 CALCULAR ESTADÍSTICAS DE USO - MODIFICADO PARA USAR CATEGORÍAS
    private fun calculateUsageStats(completed: List<PlanItem>, pending: List<PlanItem>): UsageStats {
        val allItems = completed + pending
        val totalItems = allItems.size
        val completedCount = completed.size
        val pendingCount = pending.size

        // 📌 CAMBIO PRINCIPAL: Contar por categoría (String) en lugar de ModuleType
        val categoryCounts = mutableMapOf<String, Int>()
        allItems.forEach { item ->
            // Usar la categoría real del PlanItem
            val category = item.category ?: "SIN CATEGORÍA"
            categoryCounts[category] = categoryCounts.getOrDefault(category, 0) + 1
        }

        // Mantener compatibilidad con moduleDistribution para otras partes
        val moduleStats = ModuleType.values().associate { moduleType ->
            moduleType to allItems.count { it.moduleType == moduleType }
        }

        // Días activos consecutivos
        val activeDays = calculateActiveDays(allItems)

        // Eficiencia semanal
        val weeklyEfficiency = if (totalItems > 0) {
            (completedCount.toFloat() / totalItems * 100).toInt()
        } else {
            0
        }

        return UsageStats(
            totalTasks = totalItems,
            completedTasks = completedCount,
            pendingTasks = pendingCount,
            activeDays = activeDays,
            weeklyEfficiency = weeklyEfficiency,
            moduleDistribution = moduleStats,
            productivityStreak = calculateProductivityStreak(completed),
            // 📌 NUEVO: Agregar conteo por categoría
            categoryCounts = categoryCounts
        )
    }

    // 🔔 ACTUALIZAR PREFERENCIAS CON PERSISTENCIA
    fun updatePreferences(
        notifications: Boolean? = null,
        eventReminders: Boolean? = null,
        darkMode: Boolean? = null
    ) {
        val current = _appPreferences.value
        val updated = AppPreferences(
            notificationsEnabled = notifications ?: current.notificationsEnabled,
            eventRemindersEnabled = eventReminders ?: current.eventRemindersEnabled,
            darkModeEnabled = darkMode ?: current.darkModeEnabled
        )
        _appPreferences.value = updated

        // Guardar en SharedPreferences
        savePreferencesToStorage(updated)
    }

    // 📈 MÉTODOS DE CÁLCULO
    private fun calculateActiveDays(items: List<PlanItem>): Int {
        return minOf(items.distinctBy { it.date }.size, 45)
    }

    private fun calculateProductivityStreak(completed: List<PlanItem>): Int {
        return minOf(completed.size / 2, 15)
    }

    // 💾 PERSISTENCIA - PERFIL
    private fun loadProfileFromPreferences() {
        prefs?.let { preferences ->
            val userName = preferences.getString("user_name", "Usuario AulaHelpIA") ?: "Usuario AulaHelpIA"
            val userEmail = preferences.getString("user_email", "usuario@aulahelpia.com") ?: "usuario@aulahelpia.com"
            val fullName = preferences.getString("full_name", "Nombre Completo") ?: "Nombre Completo"
            val memberSince = preferences.getString("member_since", getMemberSince()) ?: getMemberSince()

            _currentProfile.value = UserProfile(
                userName = userName,
                userEmail = userEmail,
                fullName = fullName,
                memberSince = memberSince,
                lastUpdate = "Cargado desde guardado"
            )
        }
    }

    private fun saveProfileToPreferences(profile: UserProfile) {
        prefs?.edit()?.apply {
            putString("user_name", profile.userName)
            putString("user_email", profile.userEmail)
            putString("full_name", profile.fullName)
            putString("member_since", profile.memberSince)
            putLong("profile_last_update", System.currentTimeMillis())
            apply()
        }
    }

    // 💾 PERSISTENCIA - PREFERENCIAS
    private fun loadPreferencesFromStorage() {
        prefs?.let { preferences ->
            val notifications = preferences.getBoolean("notifications_enabled", true)
            val reminders = preferences.getBoolean("event_reminders_enabled", true)
            val darkMode = preferences.getBoolean("dark_mode_enabled", false)

            _appPreferences.value = AppPreferences(
                notificationsEnabled = notifications,
                eventRemindersEnabled = reminders,
                darkModeEnabled = darkMode
            )
        }
    }

    private fun savePreferencesToStorage(prefs: AppPreferences) {
        this.prefs?.edit()?.apply {
            putBoolean("notifications_enabled", prefs.notificationsEnabled)
            putBoolean("event_reminders_enabled", prefs.eventRemindersEnabled)
            putBoolean("dark_mode_enabled", prefs.darkModeEnabled)
            apply()
        }
    }

    // 🗑️ ELIMINAR TODOS LOS DATOS
    fun deleteAllUserData() {
        viewModelScope.launch {
            eventDao.deleteAllEvents()
            // Reiniciar estadísticas
            _usageStats.value = UsageStats()

            // Limpiar SharedPreferences
            prefs?.edit()?.clear()?.apply()

            // Resetear perfil a valores por defecto
            _currentProfile.value = UserProfile()
        }
    }

    // 🕐 MÉTODOS DE AYUDA
    private fun getMemberSince(): String {
        return prefs?.getString("member_since", null) ?: run {
            val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            formatter.format(Date())
        }
    }

    // 🆕 OBTENER EMOJI PARA CATEGORÍA
    private fun getEmojiForCategory(category: String): String {
        return when (category.uppercase()) {
            "TRABAJO" -> "💼"
            "SALUD" -> "💪"
            "APRENDIZAJE" -> "📚"
            "PERSONAL" -> "😊"
            "PRIORIDAD" -> "🎯"
            "EJERCICIO" -> "🏃"
            "NUTRICIÓN" -> "🥗"
            "ESTUDIO" -> "📖"
            else -> "📝"
        }
    }

    // 🆕 OBTENER NOMBRE PARA MOSTRAR DE CATEGORÍA
    fun getCategoryDisplayName(category: String): String {
        return when (category.uppercase()) {
            "TRABAJO" -> "Trabajo"
            "SALUD" -> "Salud"
            "APRENDIZAJE" -> "Aprendizaje"
            "PERSONAL" -> "Personal"
            "PRIORIDAD" -> "Prioridad"
            "EJERCICIO" -> "Ejercicio"
            "NUTRICIÓN" -> "Nutrición"
            "ESTUDIO" -> "Estudio"
            else -> category
        }
    }
}

// 🎯 DATA CLASS PARA PERFIL DE USUARIO
data class UserProfile(
    val userName: String = "Usuario AulaHelpIA",
    val userEmail: String = "usuario@aulahelpia.com",
    val fullName: String = "Nombre Completo",
    val memberSince: String = "Nov 2024",
    val lastUpdate: String = "Recién comenzado"
)

// 📊 DATA CLASS PARA ESTADÍSTICAS DE USO - MODIFICADO
data class UsageStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val activeDays: Int = 0,
    val weeklyEfficiency: Int = 0,
    val moduleDistribution: Map<ModuleType, Int> = emptyMap(),
    val productivityStreak: Int = 0,
    // 📌 NUEVO: Conteo por categorías reales
    val categoryCounts: Map<String, Int> = emptyMap()
)

// ⚙️ DATA CLASS PARA PREFERENCIAS DE LA APP
data class AppPreferences(
    val notificationsEnabled: Boolean = true,
    val eventRemindersEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false
)