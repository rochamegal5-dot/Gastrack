package com.megal.gastrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        if (prefs.getBoolean("sesion_iniciada", false)) {
            startActivity(Intent(this, MainActivity::class.java)); finish(); return
        }

        fun login(nombre: String, id: String) {
            try {
                Toast.makeText(this, "Iniciando $nombre", Toast.LENGTH_SHORT).show()
                prefs.edit().putBoolean("sesion_iniciada", true).putString("usuario", nombre).putString("deviceId", id).apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnAntonio).setOnClickListener { login("Antonio - Equipo 7", "7") }
        findViewById<Button>(R.id.btnJose).setOnClickListener { login("Jose - Equipo 8", "8") }
        findViewById<Button>(R.id.btnMegal).setOnClickListener { login("Megal", "1") }
    }
}
