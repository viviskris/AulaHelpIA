package com.tuusuario.aulahelpia.home.utils

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.tuusuario.aulahelpia.R

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isMenuOpen = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
    }

    private fun createBubble() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = inflater.inflate(R.layout.view_floating_bubble, null)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        windowManager.addView(bubbleView, params)

        setupDraggable()
        setupMainBubbleClick()
        setupTutorBubbles()
    }

    private fun setupDraggable() {
        val mainBubble = bubbleView?.findViewById<LinearLayout>(R.id.bubbleMain)

        mainBubble?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = params!!.x
                    initialY = params!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                        if (isMenuOpen) {
                            closeMenu()
                        }
                    }

                    params!!.x = initialX + dx
                    params!!.y = initialY + dy
                    windowManager.updateViewLayout(bubbleView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMainBubbleClick() {
        val mainBubble = bubbleView?.findViewById<LinearLayout>(R.id.bubbleMain)
        mainBubble?.setOnClickListener {
            if (!isDragging) {
                toggleMenu()
            }
        }
    }

    private fun toggleMenu() {
        if (isMenuOpen) {
            closeMenu()
        } else {
            openMenu()
        }
    }

    private fun openMenu() {
        isMenuOpen = true
        val menuLayout = bubbleView?.findViewById<LinearLayout>(R.id.bubbleMenu)
        menuLayout?.visibility = View.VISIBLE

        val icon = bubbleView?.findViewById<ImageView>(R.id.ivBubbleIcon)
        icon?.setImageResource(R.drawable.ic_menu_open)
    }

    private fun closeMenu() {
        isMenuOpen = false
        val menuLayout = bubbleView?.findViewById<LinearLayout>(R.id.bubbleMenu)
        menuLayout?.visibility = View.GONE

        val icon = bubbleView?.findViewById<ImageView>(R.id.ivBubbleIcon)
        icon?.setImageResource(R.drawable.ic_menu_closed)
    }

    private fun setupTutorBubbles() {
        val container = bubbleView?.findViewById<LinearLayout>(R.id.bubbleItemsContainer)
        container?.removeAllViews()

        val materias = MateriasUtils.getMaterias(this)

        if (materias.isEmpty()) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_tutor_bubble, container, false)
            val tvEmoji = itemView.findViewById<TextView>(R.id.tvTutorEmoji)
            val tvName = itemView.findViewById<TextView>(R.id.tvTutorName)
            tvEmoji.text = "📌"
            tvName.text = "Sin materias"
            itemView.isEnabled = false
            container?.addView(itemView)
            return
        }

        for (materia in materias) {
            val emoji = MateriasUtils.getEmojiForMateria(materia)
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_tutor_bubble, container, false)
            val tvEmoji = itemView.findViewById<TextView>(R.id.tvTutorEmoji)
            val tvName = itemView.findViewById<TextView>(R.id.tvTutorName)

            tvEmoji.text = emoji
            tvName.text = materia

            val activo = materia == "Matemáticas"
            if (activo) {
                itemView.setBackgroundResource(R.drawable.bg_floating_bubble_small)
            } else {
                itemView.setBackgroundResource(R.drawable.bg_floating_bubble_small_inactive)
            }

            itemView.setOnClickListener {
                if (activo) {
                    // Abrir el Tutor de Matemáticas
                    closeMenu()
                    val intent = Intent(this, com.tuusuario.aulahelpia.MainActivity::class.java).apply {
                        putExtra("openTutorMatematicas", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "⏳ $materia (En desarrollo)", Toast.LENGTH_SHORT).show()
                    closeMenu()
                }
            }

            container?.addView(itemView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Solo remover la vista si el servicio se detiene por:
        // 1. Cierre de la app (onDestroy de MainActivity)
        // 2. Botón "Desactivar Tutores"
        // Si el servicio se reinicia, la vista se mantiene
        try {
            bubbleView?.let {
                windowManager.removeView(it)
            }
        } catch (e: Exception) {
            // La vista ya puede haber sido removida
        }
        bubbleView = null
    }
}