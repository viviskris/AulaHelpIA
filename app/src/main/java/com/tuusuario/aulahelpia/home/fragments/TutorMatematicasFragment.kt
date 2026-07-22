package com.tuusuario.aulahelpia.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.databinding.FragmentTutorMatematicasBinding
import com.tuusuario.aulahelpia.home.adapters.ChatAdapter
import com.tuusuario.aulahelpia.home.adapters.ChatMessage
import com.tuusuario.aulahelpia.home.utils.GeminiService
import kotlinx.coroutines.*
import com.tuusuario.aulahelpia.home.utils.MateriasUtils

class TutorMatematicasFragment : Fragment() {

    private var _binding: FragmentTutorMatematicasBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var geminiService: GeminiService

    private val conversationHistory = mutableListOf<String>()

    private val studentGrade = "11" // Cambiar según el grado del estudiante

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorMatematicasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geminiService = GeminiService(requireContext())

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        addWelcomeMessage()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
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
            📐 ¡Hola! Soy tu Tutor de Matemáticas.
            
            No te daré respuestas directas. Te guiaré con preguntas y pistas para que llegues a la solución por ti mismo.
            
            Ejemplos de preguntas que puedes hacer:
            • ¿Cómo resuelvo 2x + 5 = 15?
            • Explica el teorema de Pitágoras
            • ¿Cómo se resuelve una ecuación cuadrática?
            
            ¡Empieza con tu pregunta!
        """.trimIndent()

        messages.add(ChatMessage(welcomeMessage, isUser = false))
        chatAdapter.notifyItemInserted(0)
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Escribe una pregunta de matemáticas", Toast.LENGTH_SHORT).show()
            return
        }

        // Agregar al historial
        conversationHistory.add("Estudiante: $message")

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

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Construir el prompt con el historial
                val historialTexto = conversationHistory.joinToString("\n")

                val prompt = """
                Eres un tutor de matemáticas para estudiantes de grado $studentGrade.
                
                El estudiante tiene las siguientes materias:
                ${MateriasUtils.getMaterias(requireContext()).joinToString(", ")}
               
                
                REGLAS ESTRICTAS:
                1. NUNCA des la respuesta directa.
                2. Siempre guía al estudiante con preguntas y pistas.
                3. Explica conceptos paso a paso cuando sea necesario.
                4. Si el estudiante se equivoca, señala el error y dale una pista.
                5. Siempre termina con una pregunta que invite al estudiante a reflexionar.
                6. Usa ejemplos claros y lenguaje sencillo.
                7. Adapta tu explicación al nivel del estudiante.
                8. Recuerda la conversación anterior para dar respuestas coherentes.
                
                HISTORIAL DE LA CONVERSACIÓN:
                $historialTexto
                
                Pregunta del estudiante:
                $message
                
                Recuerda: eres un tutor, no un resolvedor. Guía al estudiante hacia la solución.
            """.trimIndent()

                val response = withContext(Dispatchers.IO) {
                    geminiService.generateResponse(prompt)
                }

                // Agregar respuesta al historial
                conversationHistory.add("Tutor: $response")

                // Eliminar mensaje de "escribiendo..."
                messages.removeAt(loadingIndex)
                chatAdapter.notifyItemRemoved(loadingIndex)

                // Agregar respuesta del tutor
                messages.add(ChatMessage(response, isUser = false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                binding.rvMessages.scrollToPosition(messages.size - 1)

            } catch (e: Exception) {
                messages.removeAt(loadingIndex)
                chatAdapter.notifyItemRemoved(loadingIndex)

                messages.add(ChatMessage("❌ Error: ${e.message}", isUser = false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }

            binding.btnSend.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}