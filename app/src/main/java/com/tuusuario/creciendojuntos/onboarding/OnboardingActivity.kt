package com.tuusuario.creciendojuntos.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.tuusuario.creciendojuntos.MainActivity
import com.tuusuario.creciendojuntos.R
import com.tuusuario.creciendojuntos.databinding.ActivityOnboardingBinding
import com.tuusuario.creciendojuntos.onboarding.viewmodel.OnboardingViewModel
import java.time.LocalDate

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var viewModel: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        android.util.Log.d("DEBUG", "🚀 OnboardingActivity iniciada")

        // Inicializar ViewModels
        viewModel = ViewModelProvider(this)[OnboardingViewModel::class.java]

        // Verificar si ya completó el onboarding
        if (isOnboardingCompleted()) {
            android.util.Log.d("DEBUG", "📋 Onboarding ya completado, yendo a MainActivity")
            goToMainActivity()
            return
        }

        android.util.Log.d("DEBUG", "🆕 Onboarding no completado, configurando...")
        setupNavigation()
        setupObservers()
    }

    private fun isOnboardingCompleted(): Boolean {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val completed = sharedPref.getBoolean("onboarding_completed", false)
        android.util.Log.d("DEBUG", "📊 Estado onboarding: $completed")
        return completed
    }

    private fun setupNavigation() {
        android.util.Log.d("DEBUG", "🧭 Navegación del onboarding configurada")
    }

    private fun setupObservers() {
        android.util.Log.d("DEBUG", "👀 Observadores configurados")

        // Observar cuando se complete el onboarding
        viewModel.onboardingComplete.observe(this) { complete ->
            android.util.Log.d("DEBUG", "📨 onboardingComplete observado: $complete")
            if (complete) {
                completeOnboarding()
            }
        }

        // Observar cuando se seleccione una fecha
        viewModel.selectedDueDate.observe(this) { dueDate ->
            android.util.Log.d("DEBUG", "📨 selectedDueDate observado: $dueDate")
            dueDate?.let {
                goToMainActivityWithDate(it)
            }
        }
    }

    // Función para que los fragments naveguen al siguiente paso
    fun navigateToDueDate() {
        android.util.Log.d("DEBUG", "➡️ Navegando a DueDateFragment")
    }

    // Función para completar el onboarding con fecha
    fun completeOnboardingWithDate(dueDate: LocalDate) {
        android.util.Log.d("DEBUG", "✅ Completando onboarding con fecha: $dueDate")

        // Guardar la fecha en shared preferences
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean("onboarding_completed", true)
            putString("due_date", dueDate.toString())
            apply()
        }

        android.util.Log.d("DEBUG", "💾 Datos guardados en SharedPreferences")
        goToMainActivityWithDate(dueDate)
    }

    // Función legacy para compatibilidad
    fun completeOnboarding() {
        android.util.Log.d("DEBUG", "🔄 Completando onboarding sin fecha específica")
        val defaultDueDate = LocalDate.now().plusDays(280)
        completeOnboardingWithDate(defaultDueDate)
    }

    private fun goToMainActivity() {
        android.util.Log.d("DEBUG", "🚀 Yendo a MainActivity SIN fecha")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToMainActivityWithDate(dueDate: LocalDate) {
        android.util.Log.d("DEBUG", "🚀 Yendo a MainActivity CON fecha: $dueDate")
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("DUE_DATE", dueDate.toString())
        }
        startActivity(intent)
        finish()
    }

    // Función auxiliar para que los fragments accedan al ViewModel
    fun getOnboardingViewModel(): OnboardingViewModel {
        return viewModel
    }
}