package com.megal.gastrack

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }
        val title = TextView(this).apply {
            text = "GasTrack v1.57-ATODUR\nLIVE 3s\nmegalweb.vercel.app"
            textSize = 20f
        }
        val inputEquipo = EditText(this).apply {
            hint = "Equipo ID"
            setText("7")
        }
        val inputUsuario = EditText(this).apply {
            hint = "Usuario"
            setText("Antonio")
        }
        val btnIniciar = Button(this).apply {
            text = "INICIAR ATODUR 3s"
        }
        val btnDetener = Button(this).apply {
            text = "DETENER"
        }
        status = TextView(this).apply {
            text = "Estado: Esperando..."
            textSize = 16f
        }
        btnIniciar.setOnClickListener {
            val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
            prefs.edit()
                .putString("deviceId", inputEquipo.text.toString())
                .putString("usuario", inputUsuario.text.toString())
                .putString("jornadaId", "ATODUR-${System.currentTimeMillis()}")
                .apply()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            } else {
                startForegroundService(Intent(this, LocationService::class.java))
                status.text = "ATODUR ACTIVO 3s - Enviando"
            }
        }
        btnDetener.setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            status.text = "DETENIDO"
        }
        layout.addView(title)
        layout.addView(inputEquipo)
        layout.addView(inputUsuario)
        layout.addView(btnIniciar)
        layout.addView(btnDetener)
        layout.addView(status)
        setContentView(layout)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(Intent(this, LocationService::class.java))
            status.text = "ATODUR ACTIVO 3s - Enviando"
        }
    }
}
