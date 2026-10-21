package com.megal.gastrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
            val usuario = prefs.getString("usuario", "Repartidor") ?: "Repartidor"
            val equipo = prefs.getString("deviceId", "7") ?: "7"

            findViewById<TextView>(R.id.tvBienvenido)?.let { it.text = "Hola, $usuario" }
            findViewById<TextView>(R.id.tvEquipo)?.let { it.text = "Equipo $equipo" }

            findViewById<Button>(R.id.btnIniciar)?.setOnClickListener {
                Toast.makeText(this, "Tracking iniciado", Toast.LENGTH_SHORT).show()
                try { startService(Intent(this, LocationService::class.java)) } catch (e: Exception) {}
            }
            findViewById<Button>(R.id.btnDetener)?.setOnClickListener {
                Toast.makeText(this, "Tracking pausado", Toast.LENGTH_SHORT).show()
                try { stopService(Intent(this, LocationService::class.java)) } catch (e: Exception) {}
            }
            findViewById<Button>(R.id.btnCerrarSesion)?.setOnClickListener {
                try { stopService(Intent(this, LocationService::class.java)) } catch (e: Exception) {}
                prefs.edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error Main: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
