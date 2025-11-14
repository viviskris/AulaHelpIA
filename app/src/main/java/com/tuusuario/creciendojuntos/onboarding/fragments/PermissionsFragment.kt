package com.tuusuario.CreciendoJuntos.onboarding.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.tuusuario.creciendojuntos.MainActivity
import com.tuusuario.creciendojuntos.R

class PermissionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnFinish).setOnClickListener {
            goToMainActivity()
        }

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun goToMainActivity() {
        // Guardar que el onboarding está completo
        val sharedPref = requireActivity().getSharedPreferences("app_prefs", 0)
        with(sharedPref.edit()) {
            putBoolean("onboarding_completed", true)
            apply()
        }

        // Ir a MainActivity
        val intent = Intent(requireActivity(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // Cerrar el onboarding
    }
}