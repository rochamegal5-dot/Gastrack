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
            text = "GasTrack v1.57-ATODUR\nLIVE 3s - megalweb.vercel.app"
            textSize = 22f
            setTextColor(0xFF00FF00.toInt())
        }

        val inputEquipo = EditText(this).apply {
            hint = "Equipo (7)"
            setText("7")
        }
        val inputUsuario = EditText(this).apply {
            hint = "Usuario"
            setText("Antonio")
        }
        val inputJornada = EditText(this).apply {
            hint = "Jornada ID"
            setText("ATODUR-${System.currentTimeMillis()}")
        }

        val btnIniciar = Button(this).apply {
            text = "● INICIAR JORNADA ATODUR 3s"
            setBackgroundColor(0xFFFF0000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val btnDetener = Button(this).apply {
            text = "■ DETENER"
        }

        val status = TextView(this).apply {
            text = "Estado: Esperando..."
            textSize = 16f
        }

        btnIniciar.setOnClickListener {
            val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
            prefs.edit().apply {
                putString("deviceId", inputEquipo.text.toString())
                putString("usuario", inputUsuario.text.toString())
                putString("jornadaId", inputJornada.text.toString())
                apply()
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
            } else {
                val intent = Intent(this, LocationService::class.java)
                ContextCompat.startForegroundService(this, intent)
                status.text = "Estado: ATODUR ACTIVO 3s - Equipo ${inputEquipo.text} - Enviando a megalweb.vercel.app/api/ingest"
            }
        }

        btnDetener.setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            status.text = "Estado: DETENIDO"
        }

        layout.addView(title)
        layout.addView(inputEquipo)
        layout.addView(inputUsuario)
        layout.addView(inputJornada)
        layout.addView(btnIniciar)
        layout.addView(btnDetener)
        layout.addView(status)
        setContentView(layout)
    }
}
