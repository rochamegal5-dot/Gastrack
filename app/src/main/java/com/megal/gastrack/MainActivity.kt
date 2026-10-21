package com.megal.gastrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", "Repartidor")
        val deviceId = prefs.getString("deviceId", "7")

        findViewById<TextView>(R.id.tvBienvenido).text = "Hola, $usuario"
        findViewById<TextView>(R.id.tvEquipo).text = "Equipo $deviceId"
        val tvEstado = findViewById<TextView>(R.id.tvEstado)

        findViewById<Button>(R.id.btnIniciar).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2)
                return@setOnClickListener
            }
            val i = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            tvEstado.text = "Enviando ubicación a Turso..."
            Toast.makeText(this, "Tracking Life360 iniciado", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            try { stopService(Intent(this, LocationService::class.java)) } catch (e: Exception) {}
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }
}
