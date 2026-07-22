package com.tuusuario.aulahelpia.home.fragments

import android.os.Bundle
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tuusuario.aulahelpia.R
import com.tuusuario.aulahelpia.databinding.FragmentDashboardBinding
import com.tuusuario.aulahelpia.home.viewmodel.TaskViewModel
import com.tuusuario.aulahelpia.home.viewmodel.TaskUiState
import com.tuusuario.aulahelpia.home.data.TaskState
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.tuusuario.aulahelpia.home.viewmodel.ViewModelFactory
import java.time.LocalTime
import com.tuusuario.aulahelpia.home.adapters.TaskAdapter
import com.tuusuario.aulahelpia.home.utils.MotivationalMessages
import androidx.core.content.ContextCompat
import android.graphics.Color
import com.android.billingclient.api.*
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.tuusuario.aulahelpia.home.data.PlanItem
import com.tuusuario.aulahelpia.home.dialogs.ReprogramCompleteDialog
import android.app.Application

class TaskDashboardFragment : Fragment() {


    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val PRODUCT_ID = "remove_ads"

    private val viewModel: TaskViewModel by viewModels {
        ViewModelFactory(requireContext(), requireContext().applicationContext as Application)
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 🆕 FILTRO LOCAL SOLO PARA DASHBOARD - NO AFECTA AL CALENDAR
    private var dashboardFilter: String = "ALL"  // "ALL", "ACTIVO", "COMPLETADO", "VENCIDO", "CATEGORY"
    private var selectedCategory: String? = null
    private var filteredTodayEvents: List<com.tuusuario.aulahelpia.home.data.PlanItem> = emptyList()

    // 🆕 NUEVO: Agregar estas variables para Billing
    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding.root.isVerticalScrollBarEnabled = true
        binding.root.isScrollContainer = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupAdMob()
        setupTaskDashboard()
        setupMotivationalMessage()
        setupBillingClient()  // 🆕 AGREGAR ESTA LÍNEA
        setupRemoveAdsButton() // 🆕 AGREGAR ESTA LÍNEA
        // checkOverdueTasks()
        println("🎯 TASK DASHBOARD - INICIALIZADO")
    }

    private fun setupAdMob() {
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewDashboard.loadAd(adRequest)
    }

    private fun setupTaskDashboard() {
        println("⚙️ TASK DASHBOARD - Configurando...")
        setupUI()
        setupObservers()
        setupClickListeners()
        println("⚙️ TASK DASHBOARD - Configuración completada")
    }

    private fun setupUI() {
        // USANDO LOS IDS CORRECTOS DEL NUEVO LAYOUT
        binding.tvPendingTasks.text = "0"
        binding.tvCompletedTasks.text = "0"
        binding.tvNextTask.text = "No hay tareas próximas"
        binding.tvEnergyLevel.text = "⚡ 100%"

        // 🆕 INICIALIZAR TEXTO DEL BOTÓN DE FILTRO
        binding.btnFilterTasks.text = "Filtro: Todas"

        println("✅ DASHBOARD - UI configurada para tareas")
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                // ✅ VERIFICAR SI LA VISTA ESTÁ ACTIVA
                if (!isAdded || view == null) return@collect

                when (uiState) {
                    is TaskUiState.Loading -> {
                        showLoadingState()
                    }
                    is TaskUiState.Success -> {
                        hideLoadingState()
                        updateTaskStats(uiState.stats)

                        // ✅ NUEVA LÍNEA AGREGADA: Verificar tareas vencidas
                        checkOverdueTasks()

                        // 🆕 CADA VEZ QUE SE ACTUALIZAN LOS DATOS, RE-APLICAR EL FILTRO
                        if (dashboardFilter != "ALL") {
                            applyLocalFilter()
                        } else {
                            updateRecyclerView()  // Actualizar normalmente si no hay filtro
                        }
                    }
                    is TaskUiState.Error -> {
                        hideLoadingState()
                        showErrorState(uiState.message)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnViewCalendar.setOnClickListener {
            navigateToTaskCalendar()
        }

        binding.btnAddTask.setOnClickListener {
            navigateToNewTask()
        }

        binding.btnFilterTasks.setOnClickListener {
            showFilterOptions()
        }
    }

    private fun navigateToNewTask() {
        try {
            findNavController().navigate(R.id.action_to_new_task)
            println("✅ Navegando a Nueva Tarea")
        } catch (e: Exception) {
            println("❌ Error navegando a Nueva Tarea: ${e.message}")
        }
    }

    private fun updateTaskStats(stats: com.tuusuario.aulahelpia.home.viewmodel.TaskStats) {
        if (_binding == null) return

        // 🆕 MOSTRAR ESTADÍSTICAS SEGÚN FILTRO ACTUAL
        when (dashboardFilter) {
            "ALL" -> {
                // Mostrar estadísticas generales
                binding.tvPendingTasks.text = stats.pending.toString()
                binding.tvCompletedTasks.text = stats.completed.toString()
            }
            "ACTIVO" -> {
                // Solo mostrar activas
                val activeCount = viewModel.getTodayEvents().count {
                    it.taskState == TaskState.ACTIVO.name
                }
                binding.tvPendingTasks.text = activeCount.toString()
                binding.tvCompletedTasks.text = "0"
            }
            "COMPLETADO" -> {
                // Solo mostrar completadas
                val completedCount = viewModel.getTodayEvents().count {
                    it.taskState == TaskState.COMPLETADO.name
                }
                binding.tvPendingTasks.text = "0"
                binding.tvCompletedTasks.text = completedCount.toString()
            }
            "VENCIDO" -> {
                // Solo mostrar vencidas
                val expiredCount = viewModel.getTodayEvents().count {
                    it.taskState == TaskState.VENCIDO.name
                }
                binding.tvPendingTasks.text = expiredCount.toString()
                binding.tvCompletedTasks.text = "0"
            }
            "CATEGORY" -> {
                // Mostrar estadísticas por categoría
                if (selectedCategory != null) {
                    val categoryEvents = viewModel.getTodayEvents().filter {
                        it.category == selectedCategory
                    }
                    val categoryActive = categoryEvents.count {
                        it.taskState == TaskState.ACTIVO.name
                    }
                    val categoryCompleted = categoryEvents.count {
                        it.taskState == TaskState.COMPLETADO.name
                    }

                    binding.tvPendingTasks.text = categoryActive.toString()
                    binding.tvCompletedTasks.text = categoryCompleted.toString()
                } else {
                    binding.tvPendingTasks.text = "0"
                    binding.tvCompletedTasks.text = "0"
                }
            }
        }

        // 🆕 PRÓXIMO EVENTO (considerando filtro actual)
        val nextEvent = if (dashboardFilter == "ALL") {
            viewModel.getNextUpcomingEvent()
        } else {
            // Buscar próximo evento según el filtro actual
            val events = if (dashboardFilter == "ALL") {
                viewModel.getTodayEvents()
            } else {
                filteredTodayEvents
            }

            val today = LocalDate.now().format(dateFormatter)
            val now = LocalTime.now()

            events.filter { event ->
                // Filtrar por estado si no es "ALL"
                val matchesFilter = when (dashboardFilter) {
                    "ACTIVO" -> event.taskState == TaskState.ACTIVO.name
                    "COMPLETADO" -> event.taskState == TaskState.COMPLETADO.name
                    "VENCIDO" -> event.taskState == TaskState.VENCIDO.name
                    "CATEGORY" -> selectedCategory == null || event.category == selectedCategory
                    else -> true
                }

                matchesFilter &&
                        // Solo eventos futuros o de hoy que aún no han pasado
                        (event.date > today || (event.date == today && LocalTime.parse(event.time).isAfter(now)))
            }.minByOrNull {
                "${it.date}${it.time}"
            }
        }

        binding.tvNextTask.text = if (nextEvent != null) {
            val time = LocalTime.parse(nextEvent.time).format(DateTimeFormatter.ofPattern("HH:mm"))
            "${nextEvent.title} ($time)"
        } else {
            "No hay eventos próximos"
        }

        // 🆕 ENERGÍA Y LOGROS (siempre basados en TODOS los eventos de hoy, sin filtro)
        binding.tvEnergyLevel.text = "⚡ ${viewModel.getEnergyLevel()}%"

        // 🆕 MANTENER LOGROS ORIGINALES
        binding.tvCompletedTasks.text = viewModel.getTodayAchievements()
    }

    private fun updateRecyclerView() {
        // 🆕 USAR EVENTOS FILTRADOS O TODOS LOS DE HOY
        val eventsToShow = if (dashboardFilter == "ALL") {
            viewModel.getTodayEvents()          // ← TODAS
        } else {
            filteredTodayEvents                 // ← TODAS
        }

        if (eventsToShow.isEmpty()) {
            binding.tvEmptyTasks.visibility = View.VISIBLE
            binding.recyclerTasks.visibility = View.GONE

            // 🆕 MENSAJE PERSONALIZADO SEGÚN FILTRO
            val message = when (dashboardFilter) {
                "ALL" -> "🎉 ¡No hay eventos para hoy!"
                "ACTIVO" -> "✅ ¡Excelente! No tienes tareas activas pendientes."
                "COMPLETADO" -> "📝 No hay tareas completadas hoy."
                "VENCIDO" -> "✨ ¡Perfecto! No hay tareas vencidas."
                "CATEGORY" -> "📂 No hay tareas en esta categoría para hoy."
                else -> "🎉 ¡No hay eventos para hoy!"
            }
            binding.tvEmptyTasks.text = "$message\nEs un buen día para planificar algo nuevo."
        } else {
            binding.tvEmptyTasks.visibility = View.GONE
            binding.recyclerTasks.visibility = View.VISIBLE

            val adapter = TaskAdapter(
                onStateChanged = { updatedEvent, newState ->
                    // 🆕 LÓGICA MEJORADA: Si es REPROGRAMADO, actualizar evento completo; si no, solo estado
                    if (newState == TaskState.REPROGRAMADO) {
                        // REPROGRAMACIÓN - Actualizar evento completo (con nueva fecha)
                        viewModel.updateEvent(updatedEvent)
                        println("📅 EVENTO REPROGRAMADO - ${updatedEvent.title} para ${updatedEvent.date}")
                    } else {
                        // CAMBIO DE ESTADO SIMPLE (COMPLETADO/VENCIDO/ACTIVO) - Solo actualizar estado
                        viewModel.updateTaskState(updatedEvent.id, newState.name)
                        println("🔄 ESTADO CAMBIADO - ${updatedEvent.title}: ${newState.name}")
                    }

                    // 🆕 IMPORTANTE: Recargar datos y re-aplicar filtro
                    viewModel.loadTasks()
                    if (dashboardFilter != "ALL") {
                        applyLocalFilter()
                    }
                },
                onDeleteClicked = { event ->
                    viewModel.deleteTask(event)
                    println("🗑️ EVENTO ELIMINADO - ${event.title}")

                    // 🆕 Recargar y re-aplicar filtro
                    viewModel.loadTasks()
                    if (dashboardFilter != "ALL") {
                        applyLocalFilter()
                    }
                }
            )

            // ✅ CRÍTICO: DESCOMENTAR ESTA LÍNEA
            binding.recyclerTasks.layoutManager = object : androidx.recyclerview.widget.LinearLayoutManager(requireContext()) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }

            binding.recyclerTasks.adapter = adapter
            adapter.submitList(eventsToShow)

            println("📱 DASHBOARD - ${eventsToShow.size} eventos mostrados (Filtro: $dashboardFilter)")
        }
    }

    // 🆕 FUNCIÓN PARA ACTUALIZAR RECYCLERVIEW CON EVENTOS FILTRADOS
    private fun updateRecyclerViewWithFilteredEvents() {
        val eventsToShow = filteredTodayEvents

        if (eventsToShow.isEmpty()) {
            binding.tvEmptyTasks.visibility = View.VISIBLE
            binding.recyclerTasks.visibility = View.GONE

            // Mensaje personalizado según filtro
            val message = when (dashboardFilter) {
                "ALL" -> "🎉 ¡No hay eventos para hoy!"
                "ACTIVO" -> "✅ ¡Excelente! No tienes tareas activas pendientes."
                "COMPLETADO" -> "📝 No hay tareas completadas hoy."
                "VENCIDO" -> "✨ ¡Perfecto! No hay tareas vencidas."
                "CATEGORY" -> "📂 No hay tareas en esta categoría para hoy."
                else -> "🎉 ¡No hay eventos para hoy!"
            }
            binding.tvEmptyTasks.text = "$message\nPlanifica algo nuevo."
        } else {
            binding.tvEmptyTasks.visibility = View.GONE
            binding.recyclerTasks.visibility = View.VISIBLE

            // Actualizar el adapter existente si existe
            val currentAdapter = binding.recyclerTasks.adapter as? TaskAdapter
            if (currentAdapter != null) {
                currentAdapter.submitList(eventsToShow)
            } else {
                // Crear nuevo adapter si no existe
                val adapter = TaskAdapter(
                    onStateChanged = { updatedEvent, newState ->
                        if (newState == TaskState.REPROGRAMADO) {
                            viewModel.updateEvent(updatedEvent)
                        } else {
                            viewModel.updateTaskState(updatedEvent.id, newState.name)
                        }
                        viewModel.loadTasks()
                        if (dashboardFilter != "ALL") {
                            applyLocalFilter()
                        }
                    },
                    onDeleteClicked = { event ->
                        viewModel.deleteTask(event)
                        viewModel.loadTasks()
                        if (dashboardFilter != "ALL") {
                            applyLocalFilter()
                        }
                    }
                )

                binding.recyclerTasks.layoutManager = object : androidx.recyclerview.widget.LinearLayoutManager(requireContext()) {
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }

                binding.recyclerTasks.adapter = adapter
                adapter.submitList(eventsToShow)
            }

            println("📱 DASHBOARD - ${eventsToShow.size} eventos filtrados mostrados (Filtro: $dashboardFilter)")
        }
    }

    private fun showLoadingState() {
        binding.tvPendingTasks.text = "..."
        binding.tvCompletedTasks.text = "..."
        binding.tvNextTask.text = "Cargando..."
    }

    private fun hideLoadingState() {
        // Los datos se actualizan automáticamente con los observers
    }

    private fun showErrorState(message: String) {
        binding.tvNextTask.text = "Error: $message"
    }

    // 🆕 FUNCIÓN DE FILTRADO MEJORADA
    private fun showFilterOptions() {
        val options = arrayOf(
            "Todas las de hoy",
            "Solo activas",
            "Solo completadas",
            "Solo vencidas",
            "Por categoría"
        )

        // Determinar índice actual
        val currentIndex = when {
            dashboardFilter == "ALL" -> 0
            dashboardFilter == "ACTIVO" -> 1
            dashboardFilter == "COMPLETADO" -> 2
            dashboardFilter == "VENCIDO" -> 3
            dashboardFilter == "CATEGORY" -> 4
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Filtrar eventos de hoy")
            .setIcon(android.R.drawable.ic_menu_sort_by_size)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                when (which) {
                    0 -> applyDashboardFilter("ALL", null)
                    1 -> applyDashboardFilter("ACTIVO", null)
                    2 -> applyDashboardFilter("COMPLETADO", null)
                    3 -> applyDashboardFilter("VENCIDO", null)
                    4 -> showCategorySelector(dialog)
                    else -> applyDashboardFilter("ALL", null)
                }
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpiar filtro") { dialog, _ ->
                applyDashboardFilter("ALL", null)
                dialog.dismiss()
            }
            .show()
    }

    // 🆕 SELECTOR DE CATEGORÍAS CON TUS CATEGORÍAS ESPECÍFICAS
    private fun showCategorySelector(dialog: android.content.DialogInterface) {
        // 🆕 OBTENER MATERIAS DEL PERFIL
        val materias = com.tuusuario.aulahelpia.home.utils.MateriasUtils.getMaterias(requireContext())

        // Obtener categorías usadas hoy que coincidan con las materias del perfil
        val todayEvents = viewModel.getTodayEvents()
        val usedCategoriesToday = todayEvents.mapNotNull { it.category }
            .distinct()
            .filter { it in materias } // Solo materias que existen en el perfil

        // 🆕 COMBINAR: Materias del perfil + categorías usadas hoy (para mantener consistencia)
        val allCategories = (materias + usedCategoriesToday).distinct().sorted()

        if (allCategories.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "No hay materias disponibles. Configura materias en el perfil.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val categoryArray = allCategories.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar materia")
            .setItems(categoryArray) { _, which ->
                val selectedCat = categoryArray[which]
                applyDashboardFilter("CATEGORY", selectedCat)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // 🆕 APLICAR FILTRO MEJORADO
    private fun applyDashboardFilter(filter: String, category: String?) {
        // Guardar filtro seleccionado
        dashboardFilter = filter
        selectedCategory = category

        // Aplicar filtro localmente
        applyLocalFilter()

        // Actualizar texto del botón
        val filterName = when (filter) {
            "ALL" -> "Todas"
            "ACTIVO" -> "Activas"
            "COMPLETADO" -> "Completadas"
            "VENCIDO" -> "Vencidas"
            "CATEGORY" -> "Cat: ${category ?: "Todas"}"
            else -> "Todas"
        }

        binding.btnFilterTasks.text = "Filtro: $filterName"

        // Mostrar confirmación
        val message = when (filter) {
            "ALL" -> "Mostrando todas las tareas de hoy"
            "CATEGORY" -> "Mostrando tareas de: ${category ?: "Todas las categorías"}"
            else -> "Mostrando: $filterName"
        }

        android.widget.Toast.makeText(
            requireContext(),
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // 🆕 FILTRAR EVENTOS LOCALMENTE MEJORADO
    private fun applyLocalFilter() {
        // Obtener eventos de hoy del ViewModel
        val todayEvents = viewModel.getTodayEvents()

        // Aplicar filtro local
        filteredTodayEvents = when (dashboardFilter) {
            "ALL" -> todayEvents
            "ACTIVO" -> todayEvents.filter { it.taskState == TaskState.ACTIVO.name }
            "COMPLETADO" -> todayEvents.filter { it.taskState == TaskState.COMPLETADO.name }
            "VENCIDO" -> todayEvents.filter { it.taskState == TaskState.VENCIDO.name }
            "CATEGORY" -> {
                if (selectedCategory != null) {
                    todayEvents.filter { it.category == selectedCategory }
                } else {
                    todayEvents
                }
            }
            else -> todayEvents
        }

        // Actualizar RecyclerView con eventos filtrados
        updateRecyclerViewWithFilteredEvents()

        // Actualizar estadísticas
        updateTaskStats(viewModel.getTaskStats())
    }
    private fun setupMotivationalMessage() {
        try {
            println("🔍 DASHBOARD - Configurando mensaje con animación")

            val message = MotivationalMessages.getDashboardMessage(requireContext())
            val prefs = requireContext().getSharedPreferences("motivational_counters", Context.MODE_PRIVATE)
            val counter = prefs.getInt("dashboard_counter", 0)

            binding.tvMotivationalDashboard?.text = message

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

            binding.tvMotivationalDashboard?.setBackgroundColor(
                ContextCompat.getColor(requireContext(), colorRes)
            )

            binding.tvMotivationalDashboard?.setTextColor(Color.WHITE)

            // 🆕 ANIMACIÓN - Usar MotivationalMessages.Animations
            binding.tvMotivationalDashboard?.let {
                MotivationalMessages.Animations.applyRandom(it, counter)
            }

            println("✅ DASHBOARD - Mensaje animado: $message")

        } catch (e: Exception) {
            println("⚠️ DASHBOARD - Error: ${e.message}")
        }
    }
    private fun navigateToTaskCalendar() {
        try {
            findNavController().navigate(R.id.navigation_calendar)
            println("✅ Navegando al calendario")
        } catch (e: Exception) {
            println("❌ Error navegando al calendario: ${e.message}")
        }
    }
    override fun onResume() {
        super.onResume()
        viewModel.loadTasks()
        // 🆕 Asegurar que el filtro se aplique al volver
        if (dashboardFilter != "ALL") {
            applyLocalFilter()
        }
    }
    // 🆕 MÉTODOS NUEVOS PARA COMPRAS

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener { billingResult, purchases ->
                handlePurchaseResult(billingResult, purchases)
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    println("✅ BillingClient conectado")
                    queryProductDetails() // CAMBIO 1: Nombre de función
                    checkExistingPurchases() // CAMBIO 2: Nueva función
                } else {
                    println("⚠️ BillingClient error: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                println("🔁 Reconectando BillingClient...")
                setupBillingClient()
            }
        })
    }

    private fun queryProductDetails() {
        println("🔍 Cargando producto desde Play Console...")

        // CAMBIO: Usa PRODUCT_ID en lugar de "android.test.purchased"
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID) // ← CAMBIO AQUÍ
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            println("🔍 Respuesta: ${billingResult.responseCode}")

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                productDetails = productDetailsList.firstOrNull()
                if (productDetails != null) {
                    // CAMBIO: Obtener precio REAL del producto
                    val price = productDetails!!.oneTimePurchaseOfferDetails?.formattedPrice ?: "$8.50"
                    println("✅ Producto cargado: ${productDetails?.name}")

                    binding.btnRemoveAds.isEnabled = true
                    binding.btnRemoveAds.text = "❤️ Quitar Anuncios - $price" // ← Muestra precio real
                } else {
                    println("❌ Producto no encontrado")
                    binding.btnRemoveAds.text = "⚠️ Producto no disponible"
                    binding.btnRemoveAds.isEnabled = false
                }
            } else {
                println("❌ Error: ${billingResult.debugMessage}")
                binding.btnRemoveAds.text = "⚠️ Error de conexión"
                binding.btnRemoveAds.isEnabled = false
            }
        }
    }

    private fun checkExistingPurchases() {
        // Verifica si el usuario ya compró antes
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPurchase = purchases.any {
                    it.products.contains(PRODUCT_ID) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (hasPurchase) {
                    // Usuario ya compró - activar estado premium
                    activatePremiumFeatures()
                }
            }
        }
    }

    private fun setupRemoveAdsButton() {
        binding.btnRemoveAds.isEnabled = false
        binding.btnRemoveAds.text = "⏳ Cargando producto..."

        binding.btnRemoveAds.setOnClickListener {
            launchPurchase() // ← CAMBIO: llama a launchPurchase() en lugar de launchTestPurchase()
        }
    }

    private fun launchPurchase() { // Cambia el nombre
        println("🔍 Intentando lanzar compra...")

        val productDetails = this.productDetails ?: run {
            println("❌ Producto no disponible")
            Toast.makeText(requireContext(), "Cargando producto...", Toast.LENGTH_SHORT).show()
            queryProductDetails()
            return
        }

        println("✅ Producto disponible, lanzando compra...")

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val response = billingClient.launchBillingFlow(requireActivity(), billingFlowParams)

        if (response.responseCode != BillingClient.BillingResponseCode.OK) {
            println("⚠️ Error: ${response.debugMessage}")
            Toast.makeText(requireContext(), "Error: ${response.debugMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handlePurchaseResult(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                // CAMBIO: Verifica PRODUCT_ID en lugar de "android.test.purchased"
                if (purchase.products.contains(PRODUCT_ID)) { // ← CAMBIO AQUÍ

                    // IMPORTANTE: Para productos reales hay que ACKNOWLEDGE
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            // Confirmar la compra con Google Play
                            acknowledgePurchase(purchase)
                        }

                        // Activar características premium
                        activatePremiumFeatures()
                        showPurchaseSuccessMessage()
                    }
                }
            }
        } else {
            println("❌ Compra fallida: ${billingResult.debugMessage}")
        }
    }
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                println("✅ Compra confirmada en Play Store")
            }
        }
    }

    private fun activatePremiumFeatures() {
        // 1. Guardar en SharedPreferences
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("ads_removed", true).apply()

        // 2. Actualizar UI
        binding.btnRemoveAds.text = "✅ Anuncios Removidos"
        binding.btnRemoveAds.isEnabled = false

        // 3. Aquí pondrías la lógica para ocultar anuncios
        // hideAds() - Tu función para ocultar anuncios

        println("✅ Características premium activadas")
    }

    private fun showPurchaseSuccessMessage() {
        AlertDialog.Builder(requireContext())
            .setTitle("✅ ¡Compra Exitosa! (Modo Prueba)")
            .setMessage("En la versión final, todos los anuncios se eliminarían permanentemente.\n\nPrecio real: \$8.50 USD")
            .setPositiveButton("¡Genial!") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cerrar conexión con BillingClient
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
        _binding = null
    }
    private fun checkOverdueTasks() {
        try {
            println("🔍 PASO 1: Entrando a checkOverdueTasks")

            val allTasks = viewModel.events.value
            println("🔍 PASO 2: allTasks obtenido, tamaño: ${allTasks?.size ?: 0}")

            if (allTasks == null) {
                println("🔍 PASO 3: allTasks es NULL")
                return
            }

            val overdueTasks = mutableListOf<PlanItem>()
            for (task in allTasks) {
                if (task.isOverdue() && !task.isCompleted && task.taskState != TaskState.COMPLETADO.name) {
                    overdueTasks.add(task)
                    println("🔍 Tarea vencida encontrada: ${task.title} - Fecha: ${task.date}")
                }
            }

            println("🔍 PASO 4: Total vencidas: ${overdueTasks.size}")

            if (overdueTasks.isNotEmpty()) {
                println("🔍 PASO 5: Mostrando diálogo para: ${overdueTasks.first().title}")
                showOverdueTaskResolution(overdueTasks.first())
            } else {
                println("🔍 PASO 5: No hay tareas vencidas")
            }

            println("🔍 PASO 6: checkOverdueTasks FIN")
        } catch (e: Exception) {
            println("❌ ERROR en checkOverdueTasks: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showOverdueTaskResolution(task: PlanItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("⏰ Tarea Vencida")
            .setMessage("""
            📋 ${task.title}
            📅 Fecha original: ${task.date}
            ⏰ Hora: ${task.time}
            
            Esta tarea no fue completada a tiempo.
            ¿Qué deseas hacer?
        """.trimIndent())
            .setPositiveButton("✅ Completar") { _, _ ->
                viewModel.updateTaskState(task.id, TaskState.COMPLETADO.name)
                viewModel.loadTasks()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    checkOverdueTasks()
                }, 500)
            }
            .setNeutralButton("📅 Reprogramar") { _, _ ->
                // ✅ REUTILIZAR EL DIÁLOGO EXISTENTE
                val dialog = ReprogramCompleteDialog(
                    originalDate = task.date,
                    originalTime = task.time,
                    onReprogramComplete = { newDate, newTime, reason ->
                        val updatedEvent = task.copy(
                            date = newDate,
                            time = newTime,
                            taskState = TaskState.REPROGRAMADO.name,
                            description = if (reason.isNullOrEmpty()) {
                                "📅 Reprogramado hasta $newDate a las $newTime"
                            } else {
                                "${task.description}\n📅 Reprogramado hasta $newDate a las $newTime\n💡 Motivo: $reason"
                            }
                        )
                        viewModel.updateEvent(updatedEvent)
                        viewModel.loadTasks()

// Esperar medio segundo antes de buscar la siguiente vencida
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            checkOverdueTasks()
                        }, 500)
                    }
                )
                dialog.show(requireActivity().supportFragmentManager, "ReprogramCompleteDialog")
            }
            .setNegativeButton("🗑️ Eliminar") { _, _ ->
                viewModel.deleteTask(task)
                viewModel.loadTasks()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    checkOverdueTasks()
                }, 500)
            }
            .setCancelable(false)
            .show()
    }

}