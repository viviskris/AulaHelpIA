package com.tuusuario.creciendojuntos.onboarding.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.onboarding.OnboardingActivity

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            // ✅ CORREGIDO: Usar Navigation Component
            findNavController().navigate(R.id.action_welcomeFragment_to_dueDateFragment)
        }

        view.findViewById<Button>(R.id.btnSkip).setOnClickListener {
            // Saltar onboarding e ir directamente a la app principal
            (activity as? OnboardingActivity)?.completeOnboarding()
        }
    }
}