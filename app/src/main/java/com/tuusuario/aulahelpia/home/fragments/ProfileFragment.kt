package com.tuusuario.aulahelpia.home.fragments

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import com.google.android.material.chip.Chip
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.databinding.FragmentProfileBinding
import com.tuusuario.aulahelpia.home.viewmodel.ProfileViewModel
import com.tuusuario.aulahelpia.home.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewOutlineProvider
import android.graphics.Outline
import android.widget.ImageView
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.graphics.Matrix
import kotlin.math.max
import com.tuusuario.aulahelpia.home.utils.MotivationalMessages
import com.tuusuario.aulahelpia.home.utils.MateriasUtils
import android.app.Application
import com.tuusuario.aulahelpia.home.data.HorarioStats
import com.tuusuario.aulahelpia.home.viewmodel.CalendarViewModel
import androidx.fragment.app.viewModels
import com.tuusuario.aulahelpia.home.data.HorarioDao
import com.tuusuario.aulahelpia.home.data.AppDatabase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding ?: throw IllegalStateException("ProfileFragment binding is null. Fragment may be destroyed.")

    // ✅ Agregar este método de seguridad
    private fun isBindingValid(): Boolean = isAdded && _binding != null

    private val viewModel: ProfileViewModel by viewModels {
        ViewModelFactory(requireContext(), requireContext().applicationContext as Application)
    }
    private val calendarViewModel: CalendarViewModel by viewModels()

    private var lastUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_GALLERY = 100
        private const val PERMISSION_REQUEST_READ_STORAGE = 101
    }


    private lateinit var horarioDao: HorarioDao
    // 📚 MATERIAS ADICIONALES Y COMUNES
    private val materiasAdicionales = mutableListOf<String>()
    private val materiasComunes = listOf(
        "Geometría", "Estadística", "Geografía", "Filosofía",
        "Teología", "Inglés", "Química", "Física",
        "Educación Física", "Artes", "Ética y Valores",
        "Religión", "Tecnología e Informática", "Economía",
        "Política", "Psicología"
    )
    private val materiasFijas get() = MateriasUtils.materiasFijas.map { it.first }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        horarioDao = AppDatabase.getDatabase(requireContext()).horarioDao()

        println("🔄 ProfileFragment - onViewCreated llamado")

        setupAdMob()
        setupUI()
        cargarEstadisticasHorario()
        setupClickListeners()
        setupObservers()
        viewModel.initializePhotoStorage()

        // 🆕 DEBUG: Ver contadores actuales ANTES de cambiar
        val debugInfo = MotivationalMessages.getCurrentCounters(requireContext())
        println("🔍 ANTES - $debugInfo")

        // Configurar mensaje motivacional
        setupMotivationalMessage()
        // Cargar materias adicionales guardadas
        loadMateriasAdicionales()

        // Configurar botón para agregar materias
        binding.btnAgregarMateria.setOnClickListener {
            mostrarDialogoAgregarMateria()
        }

        // 🆕 DEBUG: Ver contadores actuales DESPUÉS de cambiar
        val debugInfoAfter = MotivationalMessages.getCurrentCounters(requireContext())
        println("🔍 DESPUÉS - $debugInfoAfter")

        println("✅ ProfileFragment - Inicializado con mensaje motivacional")
    }
    private fun setupAdMob() {
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewProfile.loadAd(adRequest)
    }
    private fun setupUI() {
        binding.btnEditProfile.visibility = View.VISIBLE
        binding.btnDeleteAllData.visibility = View.VISIBLE
        binding.btnConfigureCategories.visibility = View.VISIBLE
        binding.btnExportData.visibility = View.VISIBLE

        binding.tvUserName.text = "Cargando..."
        binding.tvUserEmail.text = "AulaHelpIA"

        lifecycleScope.launch {
            val prefs = viewModel.appPreferences.value
            binding.switchNotifications.isChecked = prefs.notificationsEnabled
            binding.switchEventReminders.isChecked = prefs.eventRemindersEnabled
        }
    }
    private fun cargarEstadisticasHorario() {
        // Ejecutar en un hilo de fondo
        lifecycleScope.launch {
            try {
                println("📊 CARGANDO ESTADÍSTICAS - Inicio")

                // Obtener horario en hilo de fondo
                val horario = withContext(Dispatchers.IO) {
                    horarioDao.getAllHorarioList()
                }

                println("🔍 CLASES EN HORARIO: ${horario.size}")
                horario.forEach { println("   - ${it.materia} (${it.dia} ${it.horaInicio})") }

                // Actualizar UI en el hilo principal
                withContext(Dispatchers.Main) {
                    val totalClases = horario.size
                    binding.tvTotalClasesSemana.text = totalClases.toString()

                    val frecuenciaMaterias = horario
                        .groupBy { it.materia }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)
                        .toMap()

                    val materiasFrecuentes = if (frecuenciaMaterias.isNotEmpty()) {
                        frecuenciaMaterias.entries.joinToString(", ") {
                            "${it.key} (${it.value} clases)"
                        }
                    } else {
                        "Sin clases cargadas"
                    }
                    binding.tvMateriasFrecuentes.text = materiasFrecuentes

                    val progreso = if (totalClases > 0) 100 else 0
                    binding.tvProgresoSemanal.text = "$progreso%"
                    binding.progressSemanal.progress = progreso
                    binding.tvProgresoDetalle.text = "$totalClases de $totalClases clases completadas"

                    println("📊 ESTADÍSTICAS HORARIO: $totalClases clases")
                }

            } catch (e: Exception) {
                println("❌ Error cargando estadísticas: ${e.message}")
                e.printStackTrace()
                // Valores por defecto en UI
                withContext(Dispatchers.Main) {
                    binding.tvTotalClasesSemana.text = "0"
                    binding.tvMateriasFrecuentes.text = "Sin clases cargadas"
                    binding.tvProgresoSemanal.text = "0%"
                    binding.progressSemanal.progress = 0
                    binding.tvProgresoDetalle.text = "0 de 0 clases completadas"
                }
            }
        }
    }
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentProfile.collect { profile ->
                println("🔔 PROFILE - Perfil actualizado: ${profile.userName}")
                updateProfileUI(profile)
            }
        }

        lifecycleScope.launch {
            viewModel.usageStats.collect { stats ->
                // ✅ VERIFICAR ANTES DE ACTUALIZAR UI
                if (!isAdded || _binding == null) {
                    println("⚠️ usageStats: fragmento no activo, ignorando actualización")
                    return@collect
                }
                println("📊 PROFILE - Estadísticas: ${stats.completedTasks} completadas")
                updateStatsUI(stats)
                updateCategoryChips(stats)
            }
        }

        lifecycleScope.launch {
            viewModel.appPreferences.collect { prefs ->
                println("⚙️ PROFILE - Preferencias: notificaciones=${prefs.notificationsEnabled}")
                if (binding.switchNotifications.isChecked != prefs.notificationsEnabled) {
                    binding.switchNotifications.isChecked = prefs.notificationsEnabled
                }
                if (binding.switchEventReminders.isChecked != prefs.eventRemindersEnabled) {
                    binding.switchEventReminders.isChecked = prefs.eventRemindersEnabled
                }
            }
        }

        // 🆕 MODIFICADO: Observar la foto de perfil con más logs
        lifecycleScope.launch {
            viewModel.profilePhotoUri.collect { uri ->
                println("📸 PROFILE - Foto actualizada (collect): $uri")
                println("📸 ¿Es diferente al anterior?: ${uri != lastUri}")
                lastUri = uri
                updateProfilePhoto(uri)
            }
        }
    }
    private fun updateProfileUI(profile: com.tuusuario.aulahelpia.home.viewmodel.UserProfile) {
        binding.tvUserName.text = profile.userName
        binding.tvUserEmail.text = profile.userEmail
        binding.tvFullName.text = profile.fullName
        binding.tvEmail.text = profile.userEmail
        binding.tvMemberSince.text = profile.memberSince
    }
    private fun updateStatsUI(stats: com.tuusuario.aulahelpia.home.viewmodel.UsageStats) {
        // ✅ VERIFICACIÓN DE SEGURIDAD
        if (!isAdded || _binding == null) {
            println("⚠️ updateStatsUI: binding null o fragmento no añadido, ignorando")
            return
        }

        binding.tvWeeklyEfficiency.text = "${stats.weeklyEfficiency}%"
        binding.tvTasksCompleted.text = stats.completedTasks.toString()
        binding.tvProductivityStreak.text = "🔥 ${stats.productivityStreak} días"

        val progress = if (stats.totalTasks > 0) {
            (stats.completedTasks * 100) / stats.totalTasks
        } else {
            0
        }
        binding.progressMonthly.progress = progress
        binding.tvProgressText.text = "${stats.completedTasks}/${stats.totalTasks} tareas completadas"
    }

    private fun updateCategoryChips(stats: com.tuusuario.aulahelpia.home.viewmodel.UsageStats) {
        // Las materias ahora se manejan con el nuevo sistema
        // Este método se mantiene vacío para no romper dependencias
    }
    private fun updateProfilePhoto(uri: Uri?) {
        println("📸 updateProfilePhoto - URI recibido: $uri")

        if (uri == null) {
            println("❌ URI es null, mostrando icono por defecto")
            showDefaultProfileImage()
            return
        }

        lifecycleScope.launch {
            try {
                println("📸 Cargando en background...")

                val originalBitmap = withContext(Dispatchers.IO) {
                    try {
                        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    } catch (e: Exception) {
                        println("⚠️ Error método 1: ${e.message}")
                        try {
                            val filePath = uri.path
                            BitmapFactory.decodeFile(filePath)
                        } catch (e2: Exception) {
                            println("⚠️ Error método 2: ${e2.message}")
                            null
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (originalBitmap != null) {
                        println("✅ Bitmap original cargado: ${originalBitmap.width}x${originalBitmap.height}")

                        // 🆕 CONFIGURAR EL IMAGEVIEW CORRECTAMENTE
                        binding.ivProfilePhoto.apply {
                            // 1. QUITAR EL TINT BLANCO que blanquea la imagen
                            setColorFilter(null)
                            imageTintList = null
                            imageTintMode = null

                            // 2. REDUCIR el padding (no quitarlo completamente para mantener diseño)
                            setPadding(8, 8, 8, 8)  // 8dp en lugar de 20dp

                            // 3. MANTENER el fondo azul pero ajustarlo
                            background = createThinnerBackground()

                            // 4. Configurar para que la imagen LLENE el espacio disponible
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            adjustViewBounds = true
                        }

                        // 🆕 Calcular tamaño CONSIDERANDO el padding reducido
                        val targetSize = calculateOptimalSizeWithPadding(8)  // 8dp de padding

                        // Redimensionar y hacer circular
                        val squareBitmap = resizeBitmapToFill(originalBitmap, targetSize)
                        val circularBitmap = makeBitmapCircular(squareBitmap)

                        // Establecer la imagen
                        binding.ivProfilePhoto.setImageBitmap(circularBitmap)

                        println("✅ Foto configurada - Tamaño: ${targetSize}px, Padding: 8dp")
                        showToast("✅ Foto actualizada")

                    } else {
                        println("❌ No se pudo cargar el bitmap")
                        showDefaultProfileImage()
                    }
                }

            } catch (e: Exception) {
                println("❌ Error en updateProfilePhoto: ${e.message}")
                withContext(Dispatchers.Main) {
                    showDefaultProfileImage()
                }
            }
        }
    }
    // 🆕 Fondo más delgado (stroke en lugar de fill)
    private fun createThinnerBackground(): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)  // Fondo transparente
            setStroke(  // Solo borde de 6dp
                6,
                resources.getColor(R.color.primary_pastel, null)
            )
        }
    }
    // 🆕 Calcular tamaño CON padding personalizado
    private fun calculateOptimalSizeWithPadding(paddingDp: Int = 8): Int {
        return try {
            // ImageView: 100dp en el layout
            val density = resources.displayMetrics.density
            val totalSizePx = (100 * density).toInt()  // 100dp en píxeles

            // Padding en píxeles (ambos lados)
            val paddingPx = (paddingDp * 2 * density).toInt()

            // Espacio disponible para la imagen
            val availableSize = totalSizePx - paddingPx

            println("🔍 Cálculo tamaño: 100dp=${totalSizePx}px - ${paddingDp*2}dp padding=${paddingPx}px = ${availableSize}px")

            availableSize
        } catch (e: Exception) {
            println("⚠️ Error calculando: ${e.message}")
            250  // Fallback
        }
    }
    private fun calculateOptimalSize(): Int {
        return try {
            // Obtener las dimensiones del ImageView
            val viewWidth = binding.ivProfilePhoto.width
            val viewHeight = binding.ivProfilePhoto.height

            println("🔍 ImageView dimensions: ${viewWidth}x${viewHeight}")

            val optimalSize = if (viewWidth > 0 && viewHeight > 0) {
                // 🆕 AUMENTAR el tamaño para que llene completamente el círculo
                // El círculo recorta las esquinas, así que necesitamos imagen más grande
                val viewSize = min(viewWidth, viewHeight)
                (viewSize * 1.6).toInt()  // 40% más grande para compensar el recorte circular
            } else {
                // Si no tiene dimensiones aún, estimar
                val density = resources.displayMetrics.density
                val estimatedSize = (140 * density).toInt()  // 140dp estimados
                println("🔍 Usando tamaño estimado: $estimatedSize px (140dp)")
                estimatedSize
            }

            println("🔍 Tamaño óptimo calculado: $optimalSize px")
            optimalSize
        } catch (e: Exception) {
            println("⚠️ Error calculando tamaño: ${e.message}")
            400  // Fallback aumentado
        }
    }
    private fun resizeBitmapToSquare(bitmap: Bitmap, targetSize: Int): Bitmap {
        return try {
            println("🔄 Redimensionando bitmap: ${bitmap.width}x${bitmap.height} → ${targetSize}x${targetSize}")

            // 🆕 Crear un bitmap cuadrado del tamaño objetivo
            val squareBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(squareBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            // 🆕 Calcular escala para que la imagen LLENE el cuadrado (no solo quepa)
            val scale: Float
            val dx: Float
            val dy: Float

            if (bitmap.width > bitmap.height) {
                // Imagen más ancha que alta - escalar por altura
                scale = targetSize.toFloat() / bitmap.height
                val scaledWidth = bitmap.width * scale
                dx = (targetSize - scaledWidth) / 2f
                dy = 0f
            } else {
                // Imagen más alta que ancha - escalar por ancho
                scale = targetSize.toFloat() / bitmap.width
                val scaledHeight = bitmap.height * scale
                dx = 0f
                dy = (targetSize - scaledHeight) / 2f
            }

            // 🆕 Crear matriz de transformación
            val matrix = Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)

            // 🆕 Dibujar bitmap redimensionado
            canvas.drawBitmap(bitmap, matrix, paint)

            println("✅ Bitmap redimensionado a: ${squareBitmap.width}x${squareBitmap.height}")
            squareBitmap

        } catch (e: Exception) {
            println("⚠️ Error redimensionando bitmap: ${e.message}")
            bitmap // Devolver original si hay error
        }
    }
    // 🆕 FUNCIÓN para hacer bitmap circular
    private fun makeBitmapCircular(bitmap: Bitmap): Bitmap {
        return try {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            val radius = min(bitmap.width, bitmap.height) / 2f
            canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, radius, paint)

            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            output
        } catch (e: Exception) {
            println("⚠️ Error haciendo bitmap circular: ${e.message}")
            bitmap
        }
    }
    private fun resizeBitmapToFill(bitmap: Bitmap, targetSize: Int): Bitmap {
        return try {
            println("🔄 Redimensionando para LLENAR: ${bitmap.width}x${bitmap.height} → ${targetSize}x${targetSize}")

            val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            // Calcular escala para LLENAR (puede recortar bordes)
            val scaleX = targetSize.toFloat() / bitmap.width
            val scaleY = targetSize.toFloat() / bitmap.height
            val scale = max(scaleX, scaleY)  // Escala más grande para llenar

            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale

            // Calcular desplazamiento para centrar
            val dx = (targetSize - scaledWidth) / 2
            val dy = (targetSize - scaledHeight) / 2

            val matrix = Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)

            canvas.drawBitmap(bitmap, matrix, paint)
            println("✅ Bitmap redimensionado para llenar")
            result
        } catch (e: Exception) {
            println("⚠️ Error redimensionando: ${e.message}")
            bitmap
        }
    }
    // 🆕 FUNCIÓN para crear fondo circular programáticamente
    private fun createCircularBackground(): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(resources.getColor(R.color.primary_pastel, null))
            setStroke(4, resources.getColor(R.color.white, null))
        }
    }
    private fun showDefaultProfileImage() {
        binding.ivProfilePhoto.apply {
            // Icono de cámara con tint blanco
            setImageResource(android.R.drawable.ic_menu_camera)

            // Configuración consistente con fotos reales
            setPadding(8, 8, 8, 8)
            background = createThinnerBackground()  // Usar el NUEVO método
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true

            // Tint blanco SOLO para el icono
            setColorFilter(Color.WHITE)

            // Hacer circular
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val size = min(view.width, view.height)
                    outline.setRoundRect(0, 0, view.width, view.height, size / 2f)
                }
            }
        }
    }
    private fun setupClickListeners() {
        println("🖱️ Configurando click listeners")

        binding.ivProfilePhoto.setOnClickListener {
            showPhotoOptions()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePreferences(notifications = isChecked)
            showToast("🔔 Notificaciones ${if (isChecked) "activadas" else "desactivadas"}")
        }

        binding.switchEventReminders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePreferences(eventReminders = isChecked)
            showToast("⏰ Recordatorios ${if (isChecked) "activados" else "desactivados"}")
        }

        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.btnConfigureCategories.setOnClickListener {
            // Función eliminada - ahora usamos materias
            showToast("📊 Usa los filtros en el calendario para ver estadísticas")
        }

        binding.btnExportData.setOnClickListener {
            exportUserData()
        }

        binding.btnDeleteAllData.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        binding.btnActivarBurbuja.setOnClickListener {
            com.tuusuario.aulahelpia.home.utils.BubbleManager.startBubble(requireContext())
        }
        binding.btnDesactivarBurbuja.setOnClickListener {
            com.tuusuario.aulahelpia.home.utils.BubbleManager.stopBubble(requireContext())
        }
    }
    private fun navigateToEditProfile() {
        try {
            // Ya tienes EditProfileFragment funcionando
            val editProfileFragment = EditProfileFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, editProfileFragment)
                .addToBackStack("profile")
                .commit()
            println("✅ Navegando a EditProfileFragment")
        } catch (e: Exception) {
            println("❌ Error al navegar: ${e.message}")
            showToast("Error al abrir edición de perfil")
        }
    }
    private fun choosePhotoFromGallery() {
        println("📸 Iniciando selección desde galería")

        // Verificar y solicitar permisos si es necesario
        if (checkStoragePermission()) {
            openGallery()
        } else {
            requestStoragePermission()
        }
    }
    private fun checkStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa READ_MEDIA_IMAGES
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 y anteriores usan READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    private fun requestStoragePermission() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissions(permissions, PERMISSION_REQUEST_READ_STORAGE)
    }
    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
            }

            startActivityForResult(Intent.createChooser(intent, "Selecciona una foto"), REQUEST_CODE_GALLERY)
            println("✅ Galería abierta exitosamente")
        } catch (e: Exception) {
            println("❌ Error al abrir galería: ${e.message}")
            showToast("Error al abrir galería: ${e.message}")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, abrir galería
                    openGallery()
                } else {
                    showToast("❌ Se necesitan permisos para seleccionar foto")
                }
            }
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GALLERY -> {
                if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                    handleSelectedImage(data.data)
                } else {
                    println("ℹ️ Selección de galería cancelada")
                }
            }
        }
    }
    private fun handleSelectedImage(uri: Uri?) {
        println("📸 handleSelectedImage - URI: $uri")

        if (uri == null) {
            showToast("❌ No se pudo obtener la imagen")
            return
        }

        lifecycleScope.launch {
            try {
                showToast("📸 Guardando...")

                // 1. Guardar en ViewModel
                val success = viewModel.saveProfilePhotoFromUri(uri)

                if (success) {
                    showToast("✅ Foto guardada")

                    // 2. ESPERAR un momento y forzar actualización
                    delay(300) // Pequeña pausa

                    // 3. Obtener URI actual y actualizar DIRECTAMENTE
                    val currentUri = viewModel.profilePhotoUri.value
                    println("🔄 URI actual después de guardar: $currentUri")

                    // 4. Actualizar UI DIRECTAMENTE (sin depender del observador)
                    updateProfilePhoto(currentUri)

                } else {
                    showToast("❌ Error al guardar")
                }
            } catch (e: Exception) {
                println("❌ Error: ${e.message}")
                showToast("Error: ${e.message}")
            }
        }
    }
    private fun showPhotoOptions() {
        val options = arrayOf("📷 Tomar foto", "🖼️ Elegir de galería", "🗑️ Eliminar foto", "❌ Cancelar")

        AlertDialog.Builder(requireContext())
            .setTitle("Foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showToast("📷 Función de cámara en desarrollo - próximamente")
                    1 -> choosePhotoFromGallery() // 🆕 Ahora llama a la función real
                    2 -> deleteProfilePhoto()
                    3 -> dialog.dismiss()
                }
            }
            .show()
    }
    private fun deleteProfilePhoto() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar foto de perfil")
            .setMessage("¿Estás segura de que quieres eliminar tu foto de perfil?")
            .setPositiveButton("Eliminar") { dialog, which ->
                val success = viewModel.deleteProfilePhoto()
                if (success) {
                    showToast("✅ Foto eliminada")
                } else {
                    showToast("ℹ️ No había foto para eliminar")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun exportUserData() {
        val options = arrayOf("📝 Reporte Detallado", "📊 Resumen Ejecutivo", "📈 Solo Estadísticas")

        AlertDialog.Builder(requireContext())
            .setTitle("📤 Exportar Progreso")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> exportDetailedReport()
                    1 -> exportExecutiveSummary()
                    2 -> exportStatsOnly()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun exportDetailedReport() {
        lifecycleScope.launch {
            try {
                val stats = viewModel.usageStats.value
                val profile = viewModel.currentProfile.value

                val content = """
                    =================================
                    REPORTE AULAHELPIA DETALLADO
                    =================================
                    Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date())}
                    Usuario: ${profile.userName}
                    Email: ${profile.userEmail}
                    
                    ========== ESTADÍSTICAS ==========
                    • Tareas Totales: ${stats.totalTasks}
                    • Completadas: ${stats.completedTasks}
                    • Pendientes: ${stats.pendingTasks}
                    • Tasa de Completación: ${if (stats.totalTasks > 0) (stats.completedTasks * 100 / stats.totalTasks) else 0}%
                    • Eficiencia Semanal: ${stats.weeklyEfficiency}%
                    • Racha Productiva: ${stats.productivityStreak} días
                    • Días Activos: ${stats.activeDays}
                    
                    ========== POR CATEGORÍA =========
                    ${getFormattedMateriasStats(stats)}
                    
                    =================================
                    Generado automáticamente por AulaHelpIA
                    =================================
                """.trimIndent()

                showExportSuccessDialog(content, "reporte_detallado.txt")

            } catch (e: Exception) {
                showToast("❌ Error: ${e.message}")
            }
        }
    }
    private fun getFormattedMateriasStats(stats: com.tuusuario.aulahelpia.home.viewmodel.UsageStats): String {
        // Obtener materias del perfil
        val materias = MateriasUtils.getMaterias(requireContext())

        // Contar tareas por materia (simulación, basado en datos disponibles)
        val materiasCount = materias.associateWith { materia ->
            // Aquí podrías contar tareas reales por materia si tienes esos datos
            // Por ahora mostramos las materias configuradas
            0
        }

        return if (materiasCount.isNotEmpty()) {
            materiasCount.entries
                .joinToString("\n") { (materia, count) ->
                    val emoji = MateriasUtils.getEmojiForMateria(materia)
                    "$emoji $materia: $count tareas"
                }
        } else {
            "No hay materias configuradas."
        }
    }
    private fun exportExecutiveSummary() {
        val stats = viewModel.usageStats.value
        val profile = viewModel.currentProfile.value

        val message = """
            📊 RESUMEN EJECUTIVO - AULAHELPIA
            
            👤 USUARIO: ${profile.userName}
            📅 FECHA: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
            
            🎯 PUNTOS CLAVE:
            • ${stats.completedTasks} tareas completadas
            • ${stats.weeklyEfficiency}% de eficiencia semanal
            • ${stats.productivityStreak} días de racha productiva
            
            📈 TENDENCIAS:
            • ${if (stats.weeklyEfficiency > 70) "✅ Alto rendimiento" else "📈 Espacio para mejorar"}
            • ${if (stats.productivityStreak > 5) "🔥 Racha consistente" else "🌟 Comienza una racha"}
            
            🎯 RECOMENDACIÓN:
            ${getRecommendation(stats)}
        """.trimIndent()

        showExportSuccessDialog(message, "resumen_ejecutivo.txt")
    }
    private fun getRecommendation(stats: com.tuusuario.aulahelpia.home.viewmodel.UsageStats): String {
        return when {
            stats.completedTasks == 0 -> "🎯 Comienza completando tu primera tarea hoy."
            stats.weeklyEfficiency < 50 -> "📈 Enfócate en completar al menos la mitad de tus tareas."
            stats.productivityStreak < 3 -> "🔥 Intenta mantener una racha de 3 días seguidos."
            else -> "✅ ¡Excelente trabajo! Mantén este ritmo."
        }
    }
    private fun exportStatsOnly() {
        val stats = viewModel.usageStats.value

        val content = """
            Tareas Totales: ${stats.totalTasks}
            Completadas: ${stats.completedTasks}
            Pendientes: ${stats.pendingTasks}
            Eficiencia: ${stats.weeklyEfficiency}%
            Racha: ${stats.productivityStreak} días
            Días Activos: ${stats.activeDays}
        """.trimIndent()

        showExportSuccessDialog(content, "estadisticas.txt")
    }
    private fun showExportSuccessDialog(content: String, fileName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("✅ Exportación Exitosa")
            .setMessage("""
                📄 $fileName generado exitosamente.
                
                El contenido ha sido copiado al portapapeles.
                Puedes pegarlo en cualquier aplicación.
                
                Tamaño: ${content.length} caracteres
            """.trimIndent())
            .setPositiveButton("📋 Ver Contenido") { dialog, which ->
                showExportContentDialog(content)
            }
            .setNegativeButton("Cerrar") { dialog, which ->
                copyToClipboard(content)
            }
            .show()
    }
    private fun showExportContentDialog(content: String) {
        val displayContent = if (content.length > 1000) {
            content.take(1000) + "\n\n... [Contenido recortado para visualización]"
        } else {
            content
        }

        AlertDialog.Builder(requireContext())
            .setTitle("📋 Contenido del Reporte")
            .setMessage(displayContent)
            .setPositiveButton("📋 Copiar") { dialog, which ->
                copyToClipboard(content)
                showToast("✅ Copiado al portapapeles")
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    private fun copyToClipboard(text: String) {
        try {
            val clipboard = android.content.Context.CLIPBOARD_SERVICE
            val clip = android.content.ClipData.newPlainText("AulaHelpIA Report", text)
            val clipboardManager = requireContext().getSystemService(android.content.ClipboardManager::class.java)
            clipboardManager?.setPrimaryClip(clip)
            println("✅ Texto copiado al portapapeles")
        } catch (e: Exception) {
            println("❌ Error al copiar: ${e.message}")
            showToast("❌ Error al copiar")
        }
    }
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🗑️ Eliminar todos los datos")
            .setMessage("¿Estás segura de que quieres eliminar TODOS tus datos?\n\nEsto borrará:\n• Tu perfil\n• Todas las tareas\n• Configuraciones\n• Historial")
            .setPositiveButton("ELIMINAR TODO") { dialog, which ->
                deleteAllUserData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun deleteAllUserData() {
        try {
            val appPrefs = requireContext().getSharedPreferences("aulahelpia_prefs", android.content.Context.MODE_PRIVATE)
            appPrefs.edit().clear().apply()

            viewModel.deleteAllUserData()

            showToast("✅ Todos los datos eliminados")

            binding.tvUserName.text = "Usuario AulaHelpIA"
            binding.tvUserEmail.text = "usuario@aulahelpia.com"
            binding.tvFullName.text = "Nombre Completo"
            binding.tvEmail.text = "usuario@aulahelpia.com"

            // Los chips viejos ya no existen, se manejan con el nuevo sistema de materias

            binding.tvActivePlansCount.text = "No hay tareas activas"

        } catch (e: Exception) {
            showToast("❌ Error al eliminar datos: ${e.message}")
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        println("📢 Toast: $message")
    }
    // En ProfileFragment.kt - método simplificado:
    private fun setupMotivationalMessage() {
        try {
            println("🔍 PROFILE - Configurando mensaje con animación")

            // 1. 🛡️ VERIFICACIÓN CRÍTICA: Si la vista no está disponible, salir
            if (_binding == null || !isAdded) {
                println("⚠️ PROFILE - Binding null o fragmento no añadido, abortando")
                return
            }

            // Obtener mensaje
            val message = MotivationalMessages.getProfileMessage(requireContext())

            // Obtener contador para animación
            val prefs = requireContext().getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
            val counter = prefs.getInt("profile_counter", 0)

            // Actualizar texto (esto es seguro porque ya verificamos binding)
            binding.tvMotivationalMessage.text = message

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

            // Aplicar color con transición suave
            binding.tvMotivationalMessage.setBackgroundColor(
                ContextCompat.getColor(requireContext(), colorRes)
            )

            binding.tvMotivationalMessage.setTextColor(Color.WHITE)

            // 🆕 ANIMACIÓN: Usar MotivationalMessages.Animations
            MotivationalMessages.Animations.applyRandom(binding.tvMotivationalMessage, counter)

            // 2. 🛡️ ANIMACIÓN RETARDADA CON VERIFICACIÓN
            binding.tvMotivationalMessage.postDelayed({
                // VERIFICAR ANTES de ejecutar
                if (_binding == null || !isAdded) return@postDelayed

                MotivationalMessages.Animations.pulse(binding.tvMotivationalMessage)
            }, 1000)

            println("✅ PROFILE - Mensaje animado: $message (Contador: $counter)")

        } catch (e: Exception) {
            println("⚠️ PROFILE - Error: ${e.message}")
            // Fallback seguro
            if (_binding != null && isAdded) {
                binding.tvMotivationalMessage.text = "Organizando mi vida, un día a la vez ✨"
            }
        }
    }
    private fun mostrarToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    override fun onResume() {
        super.onResume()

        println("🔄 ProfileFragment - onResume llamado")

        // 🆕 Solo para debug, mostrar contadores actuales
        val debugInfo = MotivationalMessages.getCurrentCounters(requireContext())
        println("🔍 onResume - $debugInfo")

        // Si quieres que se actualice cada vez que vuelves, descomenta:
        // setupMotivationalMessage()
    }
    // 📚 MÉTODOS PARA MATERIAS

    private fun loadMateriasAdicionales() {
        val prefs = requireContext().getSharedPreferences("aulahelpia_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("materias_adicionales", emptySet())
        materiasAdicionales.clear()
        materiasAdicionales.addAll(saved ?: emptySet())
        actualizarChipsMaterias()
    }

    private fun saveMateriasAdicionales() {
        val prefs = requireContext().getSharedPreferences("aulahelpia_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("materias_adicionales", materiasAdicionales.toSet()).apply()
    }

    private fun actualizarChipsMaterias() {
        val chipGroup = binding.chipGroupMateriasAdicionales
        chipGroup.removeAllViews()

        // Configurar clics para materias fijas
        val chipFijas = listOf(
            binding.chipMatematicas,
            binding.chipLengua,
            binding.chipBiologia,
            binding.chipSociales
        )

        for ((index, chip) in chipFijas.withIndex()) {
            val (nombre, emoji) = MateriasUtils.materiasFijas[index]
            chip.text = "$emoji $nombre"
            chip.setChipBackgroundColorResource(MateriasUtils.getColorRes(nombre))
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            chip.setOnClickListener {
                mostrarOpcionesMateria(nombre, esFija = true)
            }
        }

        // Materias adicionales
        val colores = listOf(
            R.color.purple_neon,
            R.color.cyan_bright,
            R.color.important_pastel,
            R.color.personal_pastel,
            R.color.exercise_pastel,
            R.color.study_pastel
        )

        for ((index, materia) in materiasAdicionales.withIndex()) {
            val emoji = MateriasUtils.getEmojiForMateria(materia)
            val colorRes = MateriasUtils.getColorForAdicionalPorNombre(materia)

            val chip = Chip(requireContext()).apply {
                text = "$emoji $materia"
                setChipBackgroundColorResource(colorRes)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                setChipStrokeColorResource(R.color.purple_neon)
                isClickable = true
                setOnClickListener {
                    mostrarOpcionesMateria(materia, esFija = false)
                }
            }
            chipGroup.addView(chip)
        }

        val total = MateriasUtils.materiasFijas.size + materiasAdicionales.size
        binding.tvActivePlansCount.text = "$total materias configuradas"
    }

    private fun mostrarDialogoAgregarMateria() {
        val opciones = materiasComunes + "📝 Otra..."

        AlertDialog.Builder(requireContext())
            .setTitle("➕ Agregar materia")
            .setItems(opciones.toTypedArray()) { _, which ->
                when {
                    which == opciones.size - 1 -> {
                        mostrarDialogoOtraMateria()
                    }
                    else -> {
                        val materiaSeleccionada = opciones[which]
                        agregarMateria(materiaSeleccionada)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoOtraMateria() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Escribe el nombre de la materia"
            setSingleLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("📝 Otra materia")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    agregarMateria(nombre)
                } else {
                    mostrarToast("❌ El nombre no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarMateria(nombre: String) {
        if (materiasFijas.contains(nombre) || materiasAdicionales.contains(nombre)) {
            mostrarToast("⚠️ La materia '$nombre' ya está agregada")
            return
        }
        materiasAdicionales.add(nombre)
        saveMateriasAdicionales()
        actualizarChipsMaterias()
        mostrarToast("✅ Materia '$nombre' agregada")
    }

    private fun mostrarOpcionesMateria(materia: String, esFija: Boolean = false) {
        val opciones = mutableListOf("📚 Ver tareas")
        if (!esFija) {
            opciones.add("🗑️ Eliminar")
        }
        opciones.add("❌ Cancelar")

        AlertDialog.Builder(requireContext())
            .setTitle("📚 $materia")
            .setItems(opciones.toTypedArray()) { _, which ->
                when (opciones[which]) {
                    "📚 Ver tareas" -> {
                        mostrarToast("📍 Ve a la pestaña de Calendario y filtra por $materia")
                    }
                    "🗑️ Eliminar" -> {
                        // Solo para materias adicionales
                        materiasAdicionales.remove(materia)
                        saveMateriasAdicionales()
                        actualizarChipsMaterias()
                        mostrarToast("🗑️ Materia '$materia' eliminada")
                    }
                    "❌ Cancelar" -> {
                        // No hacer nada
                    }
                }
            }
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        println("🧹 ProfileFragment - View destruida")
    }
}