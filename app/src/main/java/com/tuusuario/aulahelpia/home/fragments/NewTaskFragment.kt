package com.tuusuario.aulahelpia.home.fragments

import android.app.DatePickerDialog
import android.content.Context
import androidx.core.content.ContextCompat
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.databinding.FragmentNewTaskBinding
import com.tuusuario.aulahelpia.home.utils.MateriasUtils
import com.tuusuario.aulahelpia.home.viewmodel.TaskViewModel
import com.tuusuario.aulahelpia.home.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.tuusuario.aulahelpia.home.utils.MotivationalMessages
import android.graphics.Color
import com.tuusuario.aulahelpia.home.data.ModuleType
import com.tuusuario.aulahelpia.home.utils.NotificationHelper
import android.util.Log
import com.tuusuario.aulahelpia.home.data.PlanItem
import android.app.Application
import com.tuusuario.aulahelpia.home.adapters.MateriaSpinnerAdapter
import androidx.lifecycle.ViewModelProvider

class NewTaskFragment : Fragment() {

    private var _binding: FragmentNewTaskBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskViewModel by viewModels {
        ViewModelFactory(requireContext(), requireContext().applicationContext as Application)
    }

    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale("es", "ES"))

    private var selectedDate = Calendar.getInstance()
    private var selectedTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
    }

    // 🎵 NUEVO: Variable para sonido seleccionado
    private var selectedSoundUri: String = ""
    private val REQUEST_CODE_SELECT_SOUND = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        // Recibir materia seleccionada desde el horario (si viene)
        val selectedMateriaArg = arguments?.getString("selectedMateria") ?: ""
        if (selectedMateriaArg.isNotEmpty()) {
            selectMateriaInSpinner(selectedMateriaArg)
        }

        setupClickListeners()
        setupAdMob()
        setupMotivationalMessage()
        println("🎯 NUEVA TAREA - Fragment inicializado")
    }

    // 🎵 NUEVO MÉTODO: Configurar AdMob
    private fun setupAdMob() {
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewNewTask.loadAd(adRequest)
    }

    private fun setupUI() {
        // 🆕 Obtener materias del perfil
        val materias = MateriasUtils.getMaterias(requireContext())

        // Si no hay materias, mostrar un mensaje
        if (materias.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "⚠️ Configura tus materias en el perfil primero",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        // Crear adapter personalizado para mostrar materias con colores
        val adapter = MateriaSpinnerAdapter(requireContext(), materias)
        binding.spinnerCategory.adapter = adapter

        // Establecer fecha y hora por defecto
        updateDateButton()
        updateTimeButton()

        // 🎵 NUEVO: Configurar botón de sonido
        binding.btnSelectSound.text = "🎵 Sonido por defecto"
    }

    private fun setupClickListeners() {
        // Botón Seleccionar Fecha
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Botón Seleccionar Hora
        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        // 🎵 NUEVO: Botón Seleccionar Sonido
        binding.btnSelectSound.setOnClickListener {
            selectNotificationSound()
        }

        // Botón Cancelar
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón Guardar
        binding.btnSave.setOnClickListener {
            saveTask()
        }
    }

    // 🎵 NUEVO MÉTODO: Seleccionar sonido de notificación
    private fun selectNotificationSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Seleccionar sonido de notificación")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)

        // Si ya hay un sonido seleccionado, mostrarlo como seleccionado
        if (selectedSoundUri.isNotEmpty()) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedSoundUri))
        }

        startActivityForResult(intent, REQUEST_CODE_SELECT_SOUND)
    }

    // 🎵 NUEVO MÉTODO: Manejar resultado de selección de sonido
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_SOUND) {
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                selectedSoundUri = uri?.toString() ?: ""

                // Actualizar el texto del botón
                if (selectedSoundUri.isNotEmpty()) {
                    binding.btnSelectSound.text = "🔔 Sonido personalizado"
                } else {
                    binding.btnSelectSound.text = "🔕 Sin sonido (silencio)"
                }

                println("🔊 SONIDO SELECCIONADO: $selectedSoundUri")
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateButton()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedTime.set(Calendar.MINUTE, minute)
                updateTimeButton()
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            true // 24 horas
        )
        timePicker.show()
    }

    private fun updateDateButton() {
        binding.btnSelectDate.text = "📅 ${dateFormatter.format(selectedDate.time)}"
    }

    private fun updateTimeButton() {
        binding.btnSelectTime.text = "⏰ ${timeFormatter.format(selectedTime.time)}"
    }
    private fun programarRecordatoriosParaTarea(task: PlanItem) {
        try {
            val notificationHelper = NotificationHelper(requireContext())

            // Programar los 3 recordatorios por defecto
            notificationHelper.scheduleTaskReminder(task, 30, "30min")
            notificationHelper.scheduleTaskReminder(task, 60, "1hora")
            notificationHelper.scheduleTaskReminder(task, 24 * 60, "1dia")

            Log.d("NOTIFICACIONES", "✅ Recordatorios programados para: ${task.title}")
        } catch (e: Exception) {
            Log.e("NOTIFICACIONES", "❌ Error programando recordatorios: ${e.message}")
        }
    }

    private fun saveTask() {
        println("🎯 INICIANDO GUARDADO DE TAREA")

        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDescription.text.toString().trim()

        // 🆕 Obtener materia seleccionada del Spinner
        val selectedMateria = binding.spinnerCategory.selectedItem as? String ?: ""

        println("🔍 DEBUG - Materia seleccionada: $selectedMateria")

        // 🎵 NUEVO: Log del sonido seleccionado
        println("🔊 DEBUG - Sonido seleccionado: $selectedSoundUri")

        // LOGS DE DEBUG
        println("🔍 DEBUG - Título: '$title'")
        println("🔍 DEBUG - Descripción: '$description'")
        println("🔍 DEBUG - Materia seleccionada: $selectedMateria")
        println("🔍 DEBUG - Fecha Calendar: ${selectedDate.time}")
        println("🔍 DEBUG - Hora Calendar: ${selectedTime.time}")

        // Validaciones
        if (title.isEmpty()) {
            println("❌ VALIDACIÓN - Título vacío")
            binding.etTaskTitle.error = "Ingresa un título para la tarea"
            return
        }

        if (selectedMateria.isEmpty()) {
            println("❌ VALIDACIÓN - Materia no seleccionada")
            binding.etTaskTitle.error = "Selecciona una materia"
            android.widget.Toast.makeText(
                requireContext(),
                "❌ Selecciona una materia",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        // ✅ VALIDACIÓN: No permitir fechas pasadas
        val todayCalendar = Calendar.getInstance()
        val selectedDateCalendar = Calendar.getInstance().apply {
            set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
        }

        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)

        selectedDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
        selectedDateCalendar.set(Calendar.MINUTE, 0)
        selectedDateCalendar.set(Calendar.SECOND, 0)
        selectedDateCalendar.set(Calendar.MILLISECOND, 0)

        if (selectedDateCalendar.before(todayCalendar)) {
            println("❌ VALIDACIÓN - Fecha pasada: ${selectedDate.time}")
            binding.etTaskTitle.error = "No se pueden crear tareas en fechas pasadas"
            android.widget.Toast.makeText(
                requireContext(),
                "❌ Selecciona una fecha hoy o futura",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            println("🔄 CONVIRTIENDO FECHA (modo seguro Play Console)...")

            val year = selectedDate.get(Calendar.YEAR)
            val month = selectedDate.get(Calendar.MONTH) + 1
            val day = selectedDate.get(Calendar.DAY_OF_MONTH)

            val fechaStringISO = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            val timeString = timeFormatter.format(selectedTime.time)

            println("✅ CONVERSIÓN EXITOSA Y SEGURA")
            println("🔍 DEBUG - Fecha ISO: $fechaStringISO")
            println("🔍 DEBUG - TimeString: $timeString")
            println("🔍 DEBUG - Materia final: $selectedMateria")

            println("📝 GUARDANDO EN VIEWMODEL...")

            // 🆕 Usar la materia como categoría
            viewModel.createTask(
                title = title,
                description = description,
                date = fechaStringISO,
                time = timeString,
                category = selectedMateria, // Ahora es el nombre de la materia
                notificationSoundUri = selectedSoundUri
            )

            // 🆕 Obtener ModuleType desde MateriasUtils
            val moduleType = MateriasUtils.getModuleTypeForMateria(selectedMateria)

            // Crear un PlanItem temporal para programar recordatorios
            val nuevaTarea = PlanItem(
                id = System.currentTimeMillis(),
                title = title,
                description = description,
                date = fechaStringISO,
                time = timeString,
                moduleType = moduleType,
                notificationSoundUri = selectedSoundUri
            )

            // Programar recordatorios
            programarRecordatoriosParaTarea(nuevaTarea)

            println("🎉 TAREA GUARDADA EXITOSAMENTE")

            // Mostrar mensaje con la materia
            val emoji = MateriasUtils.getEmojiForMateria(selectedMateria)
            android.widget.Toast.makeText(
                requireContext(),
                "✅ Tarea guardada en $emoji $selectedMateria",
                android.widget.Toast.LENGTH_LONG
            ).show()

            println("🔙 REGRESANDO AL DASHBOARD...")
            findNavController().navigateUp()

        } catch (e: Exception) {
            println("💥 ERROR CRÍTICO EN saveTask:")
            println("❌ Mensaje: ${e.message}")
            println("❌ Tipo: ${e.javaClass.simpleName}")
            e.printStackTrace()

            android.widget.Toast.makeText(
                requireContext(),
                "❌ Error al guardar: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    private fun setupMotivationalMessage() {
        try {
            println("🔍 NEW TASK - Configurando mensaje con animación")

            val message = MotivationalMessages.getNewTaskMessage(requireContext())
            val prefs = requireContext().getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
            val counter = prefs.getInt("newtask_counter", 0)

            binding.tvMotivationalNewTask?.text = message

            // Color rotativo
            val colors = listOf(
                R.color.primary_pastel,
                R.color.cyan_bright,
                R.color.cyan_bright,
                R.color.task_pastel,
                R.color.study_pastel
            )

            val colorIndex = if (counter > 0) (counter - 1) % colors.size else 0
            val colorRes = colors[colorIndex]

            binding.tvMotivationalNewTask?.setBackgroundColor(
                ContextCompat.getColor(requireContext(), colorRes)
            )

            binding.tvMotivationalNewTask?.setTextColor(Color.WHITE)

            // 🆕 ANIMACIÓN ESPECIAL para Nueva Tarea
            binding.tvMotivationalNewTask?.let {
                // Para nueva tarea, usamos scale (más llamativo)
                MotivationalMessages.Animations.scaleIn(it, 600L)

                // Pulse doble para destacar
                it.postDelayed({
                    MotivationalMessages.Animations.pulse(it)
                    it.postDelayed({
                        MotivationalMessages.Animations.pulse(it)
                    }, 400)
                }, 1200)
            }

            println("✅ NEW TASK - Mensaje animado: $message")

        } catch (e: Exception) {
            println("⚠️ NEW TASK - Error: ${e.message}")
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun selectMateriaInSpinner(materia: String) {
        val adapter = binding.spinnerCategory.adapter
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                val item = adapter.getItem(i) as? String
                if (item == materia) {
                    binding.spinnerCategory.setSelection(i)
                    println("✅ MATERIA SELECCIONADA: $materia (posición $i)")
                    return
                }
            }
            println("⚠️ MATERIA NO ENCONTRADA: $materia")
        }
    }
}