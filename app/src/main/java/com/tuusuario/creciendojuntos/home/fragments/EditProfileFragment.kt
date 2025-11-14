package com.tuusuario.creciendojuntos.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tuusuario.creciendojuntos.databinding.FragmentEditProfileBinding
import com.tuusuario.creciendojuntos.home.viewmodel.PregnancyViewModel

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PregnancyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Prellenar con datos actuales
        viewModel.currentProfile.value?.let { profile ->
            binding.etName.setText(profile.userName)
            binding.etFullName.setText(profile.fullName)
            binding.etEmail.setText(profile.userEmail)
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (validateInputs(name, fullName, email)) {
            // ✅ PASA EL CONTEXT para guardar en SharedPreferences
            viewModel.updateProfile(name, email, fullName, requireContext())
            showToast("Perfil actualizado correctamente")
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}