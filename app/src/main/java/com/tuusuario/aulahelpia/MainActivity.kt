package com.tuusuario.aulahelpia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.tuusuario.aulahelpia.databinding.ActivityMainBinding
import com.tuusuario.aulahelpia.home.utils.AdClickManager
import android.os.Build
import android.app.AlarmManager
import android.content.Intent
import com.tuusuario.aulahelpia.home.utils.FloatingBubbleService
import com.tuusuario.aulahelpia.home.fragments.TutorMatematicasFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var interstitialAd: InterstitialAd? = null
    private lateinit var adClickManager: AdClickManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("DEBUG", "🏁 MainActivity creada")

        // 1. Inicializar contador de clics
        adClickManager = AdClickManager(this)

        // 2. Configurar AdMob
        MobileAds.initialize(this) {
            Handler(Looper.getMainLooper()).postDelayed({
                val adRequest = AdRequest.Builder().build()
                binding.adViewMain.loadAd(adRequest)
                Log.d("ADMOB", "Banner cargado")
            }, 500)
        }

        // 3. Precargar interstitial
        setupInterstitial()

        // 4. Configurar navegación (sin interferir)
        setupNavigation()

        // 5. Permisos
        checkNotificationPermission()

        // ✅ 6. SOLICITAR PERMISO PARA ALARMAS EXACTAS (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Permiso de recordatorios")
                    .setMessage("AulaHelpIA necesita permiso para mostrarte recordatorios en el momento exacto que programes tus tareas.")
                    .setPositiveButton("Permitir") { _, _ ->
                        startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        // ✅ Verificar si viene del Tutor de Matemáticas
        if (intent?.getBooleanExtra("openTutorMatematicas", false) == true) {
            intent.removeExtra("openTutorMatematicas")
            binding.root.post {
                // Usar supportFragmentManager sin afectar el servicio
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, TutorMatematicasFragment())
                    .addToBackStack("tutor_matematicas")
                    .commit()
            }
        }
    }    override fun onBackPressed() {
        // 1. Intentar obtener el NavHostFragment principal
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        // 2. Verificar si es realmente un NavHostFragment (no un sub-fragment como EditProfileFragment)
        if (navHostFragment is NavHostFragment) {
            val navController = navHostFragment.navController
            val currentDestination = navController.currentDestination

            // 3. Si NO estamos en el Dashboard
            if (currentDestination?.id != R.id.navigation_dashboard) {
                // Navegar al Dashboard
                navController.popBackStack(R.id.navigation_dashboard, false)
            } else {
                // Si YA estamos en Dashboard, salir de la app
                finishAffinity()
            }
        } else {
            // 4. Si NO es NavHostFragment (estamos en un sub-fragment como EditProfileFragment)
            // Dejar que el sistema maneje el "Atrás" normalmente
            super.onBackPressed()
        }
    }

    private fun setupInterstitial() {
        val adRequest = AdRequest.Builder().build()
        // 🧪 ID DE PRUEBA - CAMBIAR POR ID REAL AL PUBLICAR
        // ID REAL: ca-app-pub-6126138750630663/8214608926
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("ADMOB", "✅ Interstitial cargado y listo")
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitialAd = null
                    // 🆕 AGREGAR ESTA LÍNEA para ver el código numérico
                    Log.e("ADMOB", "❌ Interstitial no cargado. Código: ${error.code}, Mensaje: ${error.message}")

                    // 🆕 OPCIONAL: Para aún más detalles en desarrollo
                    // error.toString() incluye toda la información interna
                    Log.e("ADMOB", "❌ Detalles completos: $error")
                }
            })
    }

    private fun showInterstitial() {
        try {
            // 1. 🛡️ COPIA LOCAL + verificación EXTRA de seguridad
            val adToShow = interstitialAd
            if (adToShow == null) {
                Log.w("ADMOB", "⚠️ showInterstitial: anuncio es null")
                setupInterstitial() // Intentar cargar uno nuevo
                return
            }

            // 2. 🛡️ INTENTAR configurar el callback (con try-catch interno)
            try {
                adToShow.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        Log.d("ADMOB", "✅ Interstitial cerrado por el usuario")
                        Handler(Looper.getMainLooper()).postDelayed({
                            setupInterstitial()
                        }, 2000)
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                        Log.e("ADMOB", "❌ Interstitial falló al mostrarse: ${p0.message}")
                        interstitialAd = null
                        setupInterstitial()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("ADMOB", "🎬 Interstitial mostrado (real)")
                    }
                }
            } catch (e: Exception) {
                Log.e("ADMOB", "🔥 Error configurando callback: ${e.message}")
                interstitialAd = null
                setupInterstitial()
                return
            }

            // 3. 🛡️ INTENTAR mostrar (con try-catch FINAL)
            try {
                adToShow.show(this)
                interstitialAd = null // Marcar como usado
                Log.d("ADMOB", "🎬 Llamada a ad.show() exitosa")
            } catch (e: Exception) {
                // ⚠️ ESTE ES EL CRASH REAL: ad.show() lanza excepción con anuncio inválido
                Log.e("ADMOB", "💥 CRASH PREVENIDO en ad.show(): ${e.message}")
                interstitialAd = null
                setupInterstitial()
            }

        } catch (e: Exception) {
            // 🛡️ CATCH GENERAL por si algo inesperado falla
            Log.e("ADMOB", "💀 Error general en showInterstitial: ${e.message}")
            interstitialAd = null
            setupInterstitial()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // ✅ CONFIGURACIÓN SEGURA - No reemplazar el listener original
        binding.bottomNavigation.setupWithNavController(navController)

        // ✅ AGREGAR NUESTRO CONTADOR DE CLICS SIN INTERFERIR
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            // 1. Registrar el clic
            val shouldShowAd = adClickManager.registerClick()

            // 2. Si alcanzó 5 clics, mostrar interstitial
            if (shouldShowAd) {
                showInterstitial()
            }

            // 3. Dejar que la navegación normal continue
            navController.navigate(menuItem.itemId)
            true
        }

        supportActionBar?.title = "AulaHelpIA CJ"
        Log.d("DEBUG", "🧭 Navegación configurada con contador de clics")
    }

    // Método para que otros fragments también registren clics
    fun registerAdClick(): Boolean {
        val shouldShowAd = adClickManager.registerClick()
        if (shouldShowAd) {
            showInterstitial()
        }
        return shouldShowAd
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("PERMISSIONS", "🔔 Permiso de notificaciones concedido")
                }
                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
            }
        } else {
            Log.d("PERMISSIONS", "🔔 Android <13 - Permiso automático")
        }
    }

    // Ciclo de vida del banner
    override fun onResume() {
        super.onResume()
        binding.adViewMain?.resume()
    }

    override fun onPause() {
        binding.adViewMain?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener la burbuja al cerrar la app
        try {
            val intent = Intent(this, FloatingBubbleService::class.java)
            stopService(intent)
        } catch (e: Exception) {
            // Ignorar
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
                    Log.d("PERMISSIONS", "🔔 Permiso de notificaciones concedido por usuario")
                } else {
                    Log.d("PERMISSIONS", "🔔 Permiso de notificaciones denegado por usuario")
                }
            }
        }
    }
}