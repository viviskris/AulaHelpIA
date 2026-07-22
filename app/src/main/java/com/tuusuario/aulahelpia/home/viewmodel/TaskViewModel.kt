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
import java.util.Locale
import java.text.SimpleDateFormat
import com.tuusuario.aulahelpia.home.data.TaskState

// ESTAS CLASES DEBEN ESTAR FUERA DEL VIEWMODEL
sealed class TaskUiState {
    object Loading : TaskUiState()
    data class Success(val tasks: List<PlanItem>, val stats: TaskStats) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

data class TaskStats(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val highPriority: Int,
    val completionRate: Int
)

class TaskViewModel(private val eventDao: EventDao) : ViewModel() {

    private val _events = MutableStateFlow<List<PlanItem>>(emptyList()) // 🆕 Cambiar _tasks por _events
    val events: StateFlow<List<PlanItem>> = _events.asStateFlow()

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow("ALL")
    val currentFilter: StateFlow<String> = _currentFilter.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            try {
                // 🆕 CAMBIAR: Cargar TODOS los eventos, no solo tareas
                eventDao.getAllEvents().collect { allEvents -> // 🆕 Necesitamos crear este método
                    _events.value = allEvents
                    _uiState.value = TaskUiState.Success(
                        tasks = allEvents, // 🆕 Usar todos los eventos
                        stats = getTaskStats()
                    )
                    println("📊 DASHBOARD - Eventos cargados: ${allEvents.size}")
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Error cargando eventos: ${e.message}")
            }
        }
    }
    // 🆕 MÉTODO PARA OBTENER EVENTOS DE HOY
    fun getTodayEvents(): List<PlanItem> {
        val calendar = Calendar.getInstance()
        val today = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        return _events.value.filter { it.date == today }
    }
    // 🆕 MÉTODO PARA OBTENER PRÓXIMOS EVENTOS
    // 🆕 MÉTODO MEJORADO PARA PRÓXIMO EVENTO EXACTO
    fun getNextUpcomingEvent(): PlanItem? {
        // Por ahora devolver null o el primer evento no completado
        return _events.value.firstOrNull { !it.isCompleted }
    }

    fun createTask(
        title: String,
        description: String,
        date: String,                // ← CAMBIAR a String
        time: String = "09:00",
        category: String = "general",
        priority: Int = 1,
        notificationSoundUri: String = ""
    ) {
        viewModelScope.launch {
            val taskDate = date.ifEmpty {
                // Si viene vacía, usar fecha actual en formato YYYY-MM-DD
                val calendar = java.util.Calendar.getInstance()
                String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }

            val newTask = PlanItem(
                title = title,
                description = description,
                date = taskDate,          // ← Ya es String
                time = time,
                moduleType = ModuleType.TASK,
                category = category,
                priority = priority,
                isCompleted = false,
                duration = 0,
                notificationSoundUri = notificationSoundUri
            )

            eventDao.insertEvent(newTask)
        }
    }

    fun toggleTaskCompletion(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            eventDao.updateCompletionStatus(taskId, completed)
        }
    }

    fun deleteTask(task: PlanItem) {
        viewModelScope.launch {
            eventDao.deleteEvent(task)
        }
    }

    fun setFilter(filter: String) {
        _currentFilter.value = filter
        updateUiState()
    }

    private fun updateUiState() {
        val filteredTasks = getFilteredTasks()
        _uiState.value = TaskUiState.Success(
            tasks = filteredTasks,
            stats = getTaskStats()
        )
    }
    fun updateTaskState(taskId: Long, newState: String) {
        viewModelScope.launch {
            eventDao.updateTaskState(taskId, newState)
            // Actualizar también isCompleted si es COMPLETADO
            if (newState == TaskState.COMPLETADO.name) {
                eventDao.updateTaskStateAndCompletion(taskId, newState, true)
            }
            println("🔄 ESTADO ACTUALIZADO - Task: $taskId, Estado: $newState")
        }
    }

    // 🆕 MÉTODO ACTUALIZADO PARA USAR ESTADOS
    fun getTaskStats(): TaskStats {
        val todayEvents = getTodayEvents()
        val total = todayEvents.size

        // 🆕 CONTAR POR ESTADO, NO SOLO isCompleted
        val completed = todayEvents.count {
            it.taskState == TaskState.COMPLETADO.name || it.isCompleted
        }
        val overdue = todayEvents.count {
            it.taskState == TaskState.VENCIDO.name
        }
        val pending = todayEvents.count {
            it.taskState == TaskState.ACTIVO.name
        }
        val paused = todayEvents.count {
            it.taskState == TaskState.REPROGRAMADO.name
        }

        val highPriority = todayEvents.count { it.priority >= 3 }

        // 🆕 LÓGICA MEJORADA: Completadas EXITOSAS vs Vencidas
        val successfulCompletions = todayEvents.count {
            it.taskState == TaskState.COMPLETADO.name
        }

        return TaskStats(
            total = total,
            completed = successfulCompletions, // ← Solo COMPLETADO exitoso
            pending = pending + paused, // ← Activos + REPROGRAMADOs
            highPriority = highPriority,
            completionRate = if (total > 0) (successfulCompletions.toFloat() / total * 100).toInt() else 0
        )
    }

    // 🆕 ACTUALIZAR getFilteredTasks() para usar eventos
    fun getFilteredTasks(): List<PlanItem> {
        val todayEvents = getTodayEvents()
        return when (_currentFilter.value) {
            "PENDING" -> todayEvents.filter { !it.isCompleted }
            "COMPLETED" -> todayEvents.filter { it.isCompleted }
            "HIGH_PRIORITY" -> todayEvents.filter { it.priority >= 3 }
            "ALL" -> todayEvents
            else -> todayEvents.filter { it.category == _currentFilter.value }
        }
    }
    fun getTasksByCategory(category: String): List<PlanItem> {
        return _events.value.filter { it.category == category }
    }

    fun searchTasks(query: String): List<PlanItem> {
        return _events.value.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
        }
    }
    // 🆕 MÉTODO CORREGIDO PARA ENERGÍA
    fun getEnergyLevel(): Int {
        val todayEvents = getTodayEvents()
        if (todayEvents.isEmpty()) return 100 // Día vacío = energía completa

        // 🆕 SOLO contar COMPLETADOS exitosos, excluir VENCIDOS
        val successfulCompletions = todayEvents.count {
            it.taskState == TaskState.COMPLETADO.name
        }
        val totalRelevant = todayEvents.count {
            // Solo contar tareas que "importan" para productividad
            it.taskState != TaskState.REPROGRAMADO.name
        }

        // 🆕 Evitar división por cero
        if (totalRelevant == 0) return 100

        val completionRate = (successfulCompletions.toFloat() / totalRelevant * 100).toInt()

        // 🆕 LÓGICA MEJORADA:
        return when {
            completionRate >= 80 -> 95  // 🟢 Excelente (4/5 o más)
            completionRate >= 60 -> 75  // 🟡 Bueno (3/5)
            completionRate >= 40 -> 50  // 🟠 Regular (2/5)
            completionRate >= 20 -> 30  // 🟠 Bajo (1/5)
            else -> 15                  // 🔴 Muy bajo (0/5)
        }
    }
    // 🆕 MÉTODO PARA ACTUALIZAR EVENTO COMPLETO
    fun updateEvent(updatedEvent: PlanItem) {
        viewModelScope.launch {
            eventDao.updateEvent(updatedEvent)
            loadTasks() // Recargar para actualizar UI
            println("🔄 EVENTO REPROGRAMADO - ${updatedEvent.title} -> ${updatedEvent.date} (REPROGRAMADO)")
        }
    }

    // 🆕 MÉTODO MEJORADO PARA LOGROS
    fun getTodayAchievements(): String {
        val todayEvents = getTodayEvents()
        val completed = todayEvents.count {
            it.taskState == TaskState.COMPLETADO.name
        }
        val overdue = todayEvents.count {
            it.taskState == TaskState.VENCIDO.name
        }
        val highPriorityCompleted = todayEvents.count {
            it.taskState == TaskState.COMPLETADO.name && it.priority >= 3
        }

        return when {
            completed == 0 && overdue == 0 -> "Sin logros aún"
            completed > 0 && overdue > 0 -> "✅ $completed completadas | 🔴 $overdue vencidas"
            completed >= 5 -> "🔥 ¡Día productivo! $completed tareas completadas"
            completed >= 3 -> "💪 Buen progreso: $completed tareas terminadas"
            overdue > 0 -> "⏰ $overdue tarea(s) vencida(s) - ¡Reprograma!"
            else -> "✨ $completed tarea(s) completada(s)"
        }
    }
}