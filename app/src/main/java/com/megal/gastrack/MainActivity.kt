package com.megal.gastrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        findViewById<TextView>(R.id.tvBienvenido).text = "Hola, ${prefs.getString("usuario","Repartidor")}"
        findViewById<TextView>(R.id.tvEquipo).text = "Equipo ${prefs.getString("deviceId","7")}"
        findViewById<Button>(R.id.btnIniciar).setOnClickListener {
            startService(Intent(this, LocationService::class.java))
        }
        findViewById<Button>(R.id.btnDetener).setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
        }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }
}
