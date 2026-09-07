package com.haritalar.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Haritalar\n\nNavigasyon çekirdeği hazırlanıyor.\nRadar güvenlik uyarıları: 4 km → 500 m zinciri aktif."
            textSize = 18f
            setPadding(32, 48, 32, 32)
        })
    }
}
