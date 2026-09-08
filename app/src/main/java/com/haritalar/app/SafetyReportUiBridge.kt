package com.haritalar.app

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout

/** Minimal, offline-first reporting UI. Reports are queued locally and are never treated as verified data. */
object SafetyReportUiBridge {
    private var activity: Activity? = null
    private var button: Button? = null
    private val handler = Handler(Looper.getMainLooper())
    private val visibilityPoll = object : Runnable {
        override fun run() {
            activity?.let { updateVisibility(it) }
            if (activity != null) handler.postDelayed(this, 1000L)
        }
    }

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                if (a is MainActivity) {
                    activity = a
                    ensureButton(a)
                    handler.removeCallbacks(visibilityPoll)
                    handler.post(visibilityPoll)
                }
            }
            override fun onActivityPaused(a: Activity) {
                if (activity === a) {
                    activity = null
                    handler.removeCallbacks(visibilityPoll)
                }
            }
            override fun onActivityDestroyed(a: Activity) {
                if (activity === a) {
                    activity = null
                    button = null
                    handler.removeCallbacks(visibilityPoll)
                }
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
        })
    }

    private fun ensureButton(a: Activity) {
        val root = a.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? FrameLayout ?: return
        if (button == null) {
            button = Button(a).apply {
                text = "Bildir"
                textSize = 12f
                setOnClickListener { showChoices(a) }
                elevation = 8f
            }
            root.addView(button, FrameLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                rightMargin = 18
                bottomMargin = 150
            })
        }
        updateVisibility(a)
    }

    private fun updateVisibility(a: Activity) {
        button?.visibility = if ((field(a, "navigationActive") as? Boolean) == true) View.VISIBLE else View.GONE
    }

    private fun showChoices(a: Activity) {
        val location = field(a, "lastLocation") as? Location ?: return
        val labels = arrayOf("Yeni güvenlik noktası", "Nokta kaldırılmış", "Yanlış konum", "Yanlış nokta tipi")
        val types = SafetyReportStore.Type.values()
        AlertDialog.Builder(a)
            .setTitle("Güvenlik bildirimi")
            .setItems(labels) { _, which ->
                val report = SafetyReportStore.enqueue(a, types[which], location.latitude, location.longitude)
                if (report != null) {
                    android.widget.Toast.makeText(a, "Bildiriminiz cihazda kuyruğa alındı.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun field(a: Activity, name: String): Any? = runCatching {
        MainActivity::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(a)
    }.getOrNull()
}
