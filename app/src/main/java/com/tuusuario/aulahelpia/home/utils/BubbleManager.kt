package com.tuusuario.aulahelpia.home.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object BubbleManager {

    fun startBubble(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "⚠️ Activa el permiso de superposición", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, FloatingBubbleService::class.java)
        context.startService(intent)
        Toast.makeText(context, "🧠 Burbuja de Tutores activada", Toast.LENGTH_SHORT).show()
    }

    fun stopBubble(context: Context) {
        val intent = Intent(context, FloatingBubbleService::class.java)
        context.stopService(intent)
        Toast.makeText(context, "🧠 Tutores desactivados", Toast.LENGTH_SHORT).show()
    }

    fun isBubbleRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == FloatingBubbleService::class.java.name }
    }
}