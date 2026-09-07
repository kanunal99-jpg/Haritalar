package com.haritalar.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/** Presentation layer: keeps routing logic in MainActivity and polishes the map UI. */
class UiPolishActivity : MainActivity() {
    private val blue = Color.rgb(25, 118, 210)
    private val text = Color.rgb(24, 32, 43)
    private val secondary = Color.rgb(92, 103, 116)
    private val soft = Color.rgb(244, 247, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        polish(content)
        content.postDelayed({ polish(content) }, 120)
    }

    private fun polish(root: ViewGroup) {
        fixRootSizing(root)
        walk(root)
    }

    private fun fixRootSizing(root: ViewGroup) {
        if (root.childCount == 0) return
        val frame = root.getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until frame.childCount) {
            val child = frame.getChildAt(i)
            if (child is LinearLayout) {
                val lp = child.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
                if (lp.height == 0) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                    child.layoutParams = lp
                }
            }
        }
    }

    private fun walk(view: View) {
        when (view) {
            is EditText -> styleSearch(view)
            is Button -> styleButton(view)
            is TextView -> styleText(view)
        }
        if (view is ViewGroup) {
            if (view is LinearLayout) {
                view.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) { child?.let { walk(it) } }
                    override fun onChildViewRemoved(parent: View?, child: View?) = Unit
                })
            }
            for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
    }

    private fun styleSearch(v: EditText) {
        v.setTextColor(text)
        v.setHintTextColor(Color.rgb(135, 145, 157))
        v.textSize = 16f
        v.minHeight = dp(58)
        v.setPadding(dp(18), 0, dp(14), 0)
        v.background = rounded(Color.rgb(243, 246, 249), 18f)
        v.elevation = dp(1).toFloat()
    }

    private fun styleButton(v: Button) {
        v.setAllCaps(false)
        v.setTextColor(text)
        v.textSize = 14f
        v.minHeight = dp(56)
        v.setPadding(dp(16), dp(8), dp(16), dp(8))
        v.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        v.background = rounded(soft, 16f)
        v.elevation = dp(1).toFloat()

        val lp = v.layoutParams
        if (lp is LinearLayout.LayoutParams) {
            if (lp.height > dp(100)) lp.height = dp(100)
            lp.bottomMargin = maxOf(lp.bottomMargin, dp(8))
            v.layoutParams = lp
        }

        if (v.text.toString().trim() == "Ara") {
            v.setTextColor(Color.WHITE)
            v.gravity = Gravity.CENTER
            v.textSize = 14f
            v.background = rounded(blue, 17f)
            v.elevation = dp(3).toFloat()
        }
    }

    private fun styleText(v: TextView) {
        val value = v.text?.toString().orEmpty()
        when {
            value == "Rota seçenekleri" -> {
                v.setTextColor(text)
                v.textSize = 20f
                v.setTypeface(null, Typeface.BOLD)
                v.setPadding(dp(4), dp(2), dp(4), dp(6))
            }
            value.startsWith("Hız, mesafe") -> {
                v.setTextColor(secondary)
                v.textSize = 12f
            }
            value.startsWith("TL tutarı") -> {
                v.setTextColor(secondary)
                v.textSize = 10.5f
                v.setPadding(dp(4), dp(6), dp(4), dp(2))
            }
            value.startsWith("Haritalar") || value.startsWith("GPS") || value.startsWith("Adres") || value.startsWith("Çoklu rota") -> {
                v.setTextColor(secondary)
                v.textSize = 12f
            }
        }
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius.toInt()).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
