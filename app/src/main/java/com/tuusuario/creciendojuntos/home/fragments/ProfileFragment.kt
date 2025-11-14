package com.tuusuario.creciendojuntos.home.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.databinding.FragmentProfileBinding
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PregnancyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar AdMob
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewProfile.loadAd(adRequest)

        // ✅ CARGA el perfil y fechas guardadas al iniciar
        viewModel.loadProfileFromPreferences(requireContext())

        // 🆕 AGREGAR ESTA LÍNEA:
        loadPregnancyDatesFromPreferences()

        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        // Observar perfil del usuario
        viewModel.currentProfile.observe(viewLifecycleOwner) { profile ->
            updateProfileUI(profile)
        }

        // Observar fecha de parto (para mostrar en el perfil)
        viewModel.dueDate.observe(viewLifecycleOwner) { dueDate ->
            dueDate?.let {
                binding.tvDueDateInfo.text = "Fecha de parto: ${formatDate(it)}"
            } ?: run {
                binding.tvDueDateInfo.text = "Fecha de parto: No establecida"
            }
        }

        // Observar última regla (para mostrar en el perfil)
        viewModel.lastPeriodDate.observe(viewLifecycleOwner) { lastPeriod ->
            lastPeriod?.let {
                binding.tvLastPeriodInfo.text = "Última regla: ${formatDate(it)}"
            } ?: run {
                binding.tvLastPeriodInfo.text = "Última regla: No establecida"
            }
        }
    }

    private fun setupClickListeners() {
        // Foto de perfil
        binding.ivProfilePhoto.setOnClickListener {
            showToast("Funcionalidad de foto en desarrollo")
        }

        // Gestión del embarazo
        binding.btnResetDueDate.setOnClickListener {
            showDueDatePicker()
        }

        binding.btnResetLastPeriod.setOnClickListener {
            showLastPeriodPicker()
        }

        // Preferencias
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            showToast("Notificaciones ${if (isChecked) "activadas" else "desactivadas"}")
        }

        binding.switchEventReminders.setOnCheckedChangeListener { _, isChecked ->
            showToast("Recordatorios ${if (isChecked) "activados" else "desactivados"}")
        }

        // Acciones
        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.btnConfigureMilestones.setOnClickListener {
            showToast("Navegando a hitos")
        }
    }

    private fun updateProfileUI(profile: PregnancyViewModel.UserProfile) {
        binding.tvUserName.text = profile.userName
        binding.tvUserEmail.text = profile.userEmail
        binding.tvFullName.text = profile.fullName
        binding.tvEmail.text = profile.userEmail
        binding.tvLastUpdate.text = "Última actualización: ${profile.lastUpdate}"
    }

    private fun showDueDatePicker() {
        val currentDate = viewModel.dueDate.value ?: LocalDate.now()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)

                // ✅ ACTUALIZAR en ViewModel (método PRINCIPAL que ya usas en Calendar)
                viewModel.setDueDate(selectedDate)

                // ✅ GUARDAR FECHAS EN SHAREDPREFERENCES
                viewModel.saveProfileDueDateToPreferences(requireContext())

                // 🆕 AGREGAR: Forzar guardado del perfil también
                viewModel.saveProfileToPreferences(requireContext())

                showToast("Fecha de parto actualizada")

                println("✅ PROFILE - Fecha de parto guardada: $selectedDate")
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    private fun showLastPeriodPicker() {
        val currentDate = viewModel.lastPeriodDate.value ?: LocalDate.now()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)

                // ✅ ACTUALIZAR en ViewModel (método PRINCIPAL que ya usas en Calendar)
                viewModel.setLastPeriodDate(selectedDate)

                // ✅ GUARDAR FECHAS EN SHAREDPREFERENCES
                viewModel.saveProfileDueDateToPreferences(requireContext())

                // 🆕 AGREGAR: Forzar guardado del perfil también
                viewModel.saveProfileToPreferences(requireContext())

                showToast("Fecha de última regla actualizada")

                println("✅ PROFILE - Última regla guardada: $selectedDate")
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    private fun navigateToEditProfile() {
        val editProfileFragment = EditProfileFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, editProfileFragment)
            .addToBackStack("profile")
            .commit()
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "ES"))
        return date.format(formatter)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // 🆕 MÉTODO NUEVO: Cargar fechas del embarazo desde SharedPreferences
    private fun loadPregnancyDatesFromPreferences() {
        val sharedPref = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

        val savedDueDate = sharedPref.getString("due_date", null)
        val savedLastPeriod = sharedPref.getString("last_period_date", null)

        println("🔍 PROFILE - Cargando fechas desde SharedPreferences:")
        println("   - due_date: '$savedDueDate'")
        println("   - last_period_date: '$savedLastPeriod'")

        savedDueDate?.takeIf { it.isNotBlank() }?.let {
            try {
                val dueDate = LocalDate.parse(it)
                viewModel.setDueDate(dueDate)
                println("✅ PROFILE - Fecha de parto cargada: $dueDate")
            } catch (e: Exception) {
                println("💥 PROFILE - Error parseando due_date: ${e.message}")
            }
        }

        savedLastPeriod?.takeIf { it.isNotBlank() }?.let {
            try {
                val lastPeriod = LocalDate.parse(it)
                viewModel.setLastPeriodDate(lastPeriod)
                println("✅ PROFILE - Última regla cargada: $lastPeriod")
            } catch (e: Exception) {
                println("💥 PROFILE - Error parseando last_period_date: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}