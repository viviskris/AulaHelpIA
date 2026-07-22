package com.tuusuario.aulahelpia.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.databinding.FragmentAssistantChatBinding
import com.tuusuario.aulahelpia.home.adapters.ChatAdapter
import com.tuusuario.aulahelpia.home.adapters.ChatMessage
import com.tuusuario.aulahelpia.home.utils.GeminiService
import com.tuusuario.aulahelpia.home.utils.MateriasUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AssistantChatFragment : Fragment() {

    private var _binding: FragmentAssistantChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter

    private var horarioDao: com.tuusuario.aulahelpia.home.data.HorarioDao? = null
    private var eventDao: com.tuusuario.aulahelpia.home.data.EventDao? = null
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var geminiService: GeminiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssistantChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geminiService = GeminiService(requireContext())

        val db = com.tuusuario.aulahelpia.home.data.AppDatabase.getDatabase(requireContext())
        horarioDao = db.horarioDao()
        eventDao = db.eventDao()

        setupRecyclerView()
        setupClickListeners()
        addWelcomeMessage()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = chatAdapter
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage = """
            🤖 ¡Hola! Soy tu Asistente AulaHelpIA.
            
            Puedo ayudarte a organizar tu día, priorizar tareas y planificar tu estudio.
            
            Pregúntame cosas como:
            • ¿Cómo organizo mi semana?
            • ¿Qué debo estudiar hoy?
            • Tengo 3 tareas, ¿cuál hago primero?
        """.trimIndent()

        messages.add(ChatMessage(welcomeMessage, isUser = false))
        chatAdapter.notifyItemInserted(0)
    }

    private fun sendMessage() {
        println("🔥 MENSAJE ENVIADO")  // ← AGREGAR ESTA LÍNEA
        val message = binding.etMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Escribe una pregunta", Toast.LENGTH_SHORT).show()
            return
        }

        // Agregar mensaje del usuario
        messages.add(ChatMessage(message, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.etMessage.text?.clear()
        binding.rvMessages.scrollToPosition(messages.size - 1)

        // Mostrar "escribiendo..."
        val loadingMessage = ChatMessage("✍️ Pensando...", isUser = false)
        val loadingIndex = messages.size
        messages.add(loadingMessage)
        chatAdapter.notifyItemInserted(loadingIndex)
        binding.rvMessages.scrollToPosition(messages.size - 1)

        binding.btnSend.isEnabled = false

        // Llamar a Gemini
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Obtener contexto del estudiante
                val contextInfo = getStudentContext()
                println("📊 CONTEXTO ENVIADO A GEMINI:\n$contextInfo")

                // Generar respuesta con Gemini
                val response = withContext(Dispatchers.IO) {
                    geminiService.generateResponseWithContext(message, contextInfo)
                }

                // Eliminar mensaje de "escribiendo..."
                messages.removeAt(loadingIndex)
                chatAdapter.notifyItemRemoved(loadingIndex)

                // Agregar respuesta real
                messages.add(ChatMessage(response, isUser = false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                binding.rvMessages.scrollToPosition(messages.size - 1)

            } catch (e: Exception) {
                // Eliminar mensaje de "escribiendo..."
                messages.removeAt(loadingIndex)
                chatAdapter.notifyItemRemoved(loadingIndex)

                // Mostrar error
                messages.add(ChatMessage("Lo siento, hubo un problema. Inténtalo de nuevo.", isUser = false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }

            binding.btnSend.isEnabled = true
        }
    }

    private suspend fun getStudentContext(): String {
        // 1. Obtener materias del perfil
        val materias = MateriasUtils.getMaterias(requireContext())
        println("📊 Materias: ${materias.size}")

        // 2. Obtener horario semanal
        val horario = try {
            horarioDao?.getAllHorarioList() ?: emptyList()
        } catch (e: Exception) {
            println("⚠️ Error obteniendo horario: ${e.message}")
            emptyList()
        }
        println("📊 Horario: ${horario.size}")

        // 3. Obtener tareas de hoy
        val today = android.text.format.DateFormat.format("yyyy-MM-dd", java.util.Calendar.getInstance()).toString()
        val tareasHoy: List<com.tuusuario.aulahelpia.home.data.PlanItem> = try {
            eventDao?.getEventsForDate(today)?.first() ?: emptyList()
        } catch (e: Exception) {
            println("⚠️ Error obteniendo tareas: ${e.message}")
            emptyList()
        }
        println("📊 Tareas hoy: ${tareasHoy.size}")

        // 4. Estadísticas de tareas
        val totalTareas = tareasHoy.size
        val completadas = tareasHoy.filter { it.taskState == "COMPLETADO" }.size
        val pendientes = totalTareas - completadas
        val vencidas = tareasHoy.filter { it.taskState == "VENCIDO" }.size

        // 5. Formatear horario
        val horarioTexto = if (horario.isNotEmpty()) {
            horario.joinToString("\n") {
                "• ${it.dia} ${it.horaInicio} - ${it.horaFin}: ${it.materia} (Prof: ${it.profesor})"
            }
        } else {
            "Sin horario configurado"
        }

        // 6. Formatear tareas
        val tareasTexto = if (tareasHoy.isNotEmpty()) {
            tareasHoy.joinToString("\n") {
                "• ${it.title} (${it.time}) - Estado: ${it.taskState ?: "Pendiente"}"
            }
        } else {
            "Sin tareas para hoy"
        }

        // 7. Construir contexto completo
        return """
        === DATOS DEL ESTUDIANTE ===
        
        📚 MATERIAS:
        ${materias.joinToString("\n") { "- $it" }}
        
        ⏰ HORARIO SEMANAL:
        $horarioTexto
        
        📋 TAREAS DE HOY ($today):
        $tareasTexto
        
        📊 ESTADÍSTICAS:
        • Tareas totales: $totalTareas
        • Completadas: $completadas
        • Pendientes: $pendientes
        • Vencidas: $vencidas
        
        El estudiante está en grados 10 u 11.
        La app se llama AulaHelpIA y sirve para organizar su vida académica.
        
        === FIN DEL CONTEXTO ===
    """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}