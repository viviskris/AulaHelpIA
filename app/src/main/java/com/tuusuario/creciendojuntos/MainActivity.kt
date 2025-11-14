package com.tuusuario.creciendojuntos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.tuusuario.creciendojuntos.databinding.ActivityMainBinding
// ELIMINA el import del ViewModel de aquí
import java.time.LocalDate
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // ELIMINA la declaración del ViewModel de aquí

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        android.util.Log.d("DEBUG", "🏁 MainActivity creada")

        // 🆕 AGREGAR ESTO:
        // Configurar AdMob
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adViewMain.loadAd(adRequest)

        setupNavigation()
        // COMENTA temporalmente estas líneas:
        // handleIntentData()
        // loadSavedDueDate()
        checkNotificationPermission()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        supportActionBar?.title = "Creciendo Juntos"
        android.util.Log.d("DEBUG", "🧭 Navegación configurada")
    }

    // COMENTA temporalmente estos métodos:
    /*
    private fun handleIntentData() {
        val dueDateString = intent.getStringExtra("DUE_DATE")
        android.util.Log.d("DEBUG", "📨 Intent data - DUE_DATE: $dueDateString")

        dueDateString?.let {
            try {
                val dueDate = LocalDate.parse(it)
                android.util.Log.d("DEBUG", "✅ Fecha parseada: $dueDate")
                // pregnancyViewModel.setDueDate(dueDate)
                saveDueDate(dueDate)
                android.util.Log.d("DEBUG", "✅ ViewModel actualizado con fecha")
            } catch (e: Exception) {
                android.util.Log.d("DEBUG", "❌ Error parseando fecha: ${e.message}")
                e.printStackTrace()
            }
        } ?: run {
            android.util.Log.d("DEBUG", "❌ No hay DUE_DATE en el intent")
        }
    }

    private fun loadSavedDueDate() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedDueDate = sharedPref.getString("due_date", null)
        android.util.Log.d("DEBUG", "💾 Saved due_date: $savedDueDate")

        savedDueDate?.let {
            try {
                val dueDate = LocalDate.parse(it)
                // pregnancyViewModel.setDueDate(dueDate)
                android.util.Log.d("DEBUG", "✅ Fecha cargada desde SharedPreferences: $dueDate")
            } catch (e: Exception) {
                android.util.Log.d("DEBUG", "❌ Error cargando fecha guardada: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun saveDueDate(dueDate: LocalDate) {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit().putString("due_date", dueDate.toString()).apply()
        android.util.Log.d("DEBUG", "💾 Fecha guardada: $dueDate")
    }
    */

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    println("🔔 Permiso de notificaciones concedido")
                }
                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
            }
        } else {
            println("🔔 Android <13 - Permiso automático")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    println("🔔 Permiso de notificaciones concedido por el usuario")
                } else {
                    println("🔔 Permiso de notificaciones denegado por el usuario")
                }
            }
        }
    }
}