package com.tuusuario.aulahelpia.home.fragments

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
import com.tuusuario.aulahelpia.databinding.FragmentEditProfileBinding
import com.tuusuario.aulahelpia.home.viewmodel.ProfileViewModel
import com.tuusuario.aulahelpia.home.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import android.app.Application

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ViewModelFactory(requireContext(), requireContext().applicationContext as Application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdMob()
        setupObservers()
        setupClickListeners()
    }

    private fun setupAdMob() {
        MobileAds.initialize(requireContext())
        val adRequest = AdRequest.Builder().build()
        binding.adViewEditProfile.loadAd(adRequest)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentProfile.collect { profile ->
                println("🔔 EDIT PROFILE - Cargando perfil: ${profile.userName}")
                // Solo cargar datos en los campos de edición
                binding.etName.setText(profile.userName)
                binding.etEmail.setText(profile.userEmail)
                binding.etFullName.setText(profile.fullName)
            }
        }

        lifecycleScope.launch {
            viewModel.appPreferences.collect { prefs ->
                println("⚙️ EDIT PROFILE - Preferencias: notificaciones=${prefs.notificationsEnabled}")
                if (binding.switchNotifications.isChecked != prefs.notificationsEnabled) {
                    binding.switchNotifications.isChecked = prefs.notificationsEnabled
                }
                // switchEventReminders no existe en EditProfile, lo removemos
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Guardar preferencias cuando cambian
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePreferences(notifications = isChecked)
        }

        // switchDarkMode - si existe en tu layout
        binding.switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePreferences(darkMode = isChecked)
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (validateInputs(name, fullName, email)) {
            // Actualizar perfil
            viewModel.updateProfile(name, email, fullName)

            // Guardar en SharedPreferences
            saveProfileToPreferences(name, email, fullName)

            showToast("✅ Perfil actualizado correctamente")
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun validateInputs(name: String, fullName: String, email: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.etName.error = "El nombre es requerido"
            isValid = false
        } else {
            binding.etName.error = null
        }

        if (fullName.isEmpty()) {
            binding.etFullName.error = "El nombre completo es requerido"
            isValid = false
        } else {
            binding.etFullName.error = null
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "El email es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Ingresa un email válido"
            isValid = false
        } else {
            binding.etEmail.error = null
        }

        return isValid
    }

    private fun saveProfileToPreferences(name: String, email: String, fullName: String) {
        val prefs = requireContext().getSharedPreferences("aulahelpia_prefs", android.content.Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("user_name", name)
            putString("user_email", email)
            putString("full_name", fullName)
            putLong("last_update", System.currentTimeMillis())
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}