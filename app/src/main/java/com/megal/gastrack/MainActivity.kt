package com.megal.gastrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }
        val title = TextView(this).apply {
            text = "GasTrack v1.57-ATODUR\nLIVE 3s"
            textSize = 22f
        }
        val inputEquipo = EditText(this).apply { hint = "Equipo"; setText("7") }
        val inputUsuario = EditText(this).apply { hint = "Usuario"; setText("Antonio") }
        val btnIniciar = Button(this).apply { text = "INICIAR ATODUR 3s" }
        val btnDetener = Button(this).apply { text = "DETENER" }
        val status = TextView(this).apply { text = "Estado: Esperando..." }

        btnIniciar.setOnClickListener {
            val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
            prefs.edit().putString("deviceId", inputEquipo.text.toString())
                .putString("usuario", inputUsuario.text.toString())
                .putString("jornadaId", "ATODUR-${System.currentTimeMillis()}").apply()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                ContextCompat.startForegroundService(this, Intent(this, LocationService::class.java))
                status.text = "ATODUR ACTIVO 3s"
            }
        }
        btnDetener.setOnClickListener { stopService(Intent(this, LocationService::class.java)); status.text = "DETENIDO" }
        layout.addView(title); layout.addView(inputEquipo); layout.addView(inputUsuario)
        layout.addView(btnIniciar); layout.addView(btnDetener); layout.addView(status)
        setContentView(layout)
    }
}
