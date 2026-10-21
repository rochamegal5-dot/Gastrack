package com.megal.gastrack

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var tvBienvenido: TextView
    private lateinit var tvEquipo: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnDetener: Button
    private lateinit var btnCerrarSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("gastrack", MODE_PRIVATE)

        if (!prefs.getBoolean("sesion_iniciada", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvBienvenido = findViewById(R.id.tvBienvenido)
        tvEquipo = findViewById(R.id.tvEquipo)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        val nombre = prefs.getString("usuario", "Repartidor")
        val deviceId = prefs.getString("deviceId", "7")
        tvBienvenido.text = "Hola, $nombre"
        tvEquipo.text = "Equipo $deviceId - ${prefs.getString("telefono","")}"

        btnIniciar.setOnClickListener {
            if (!checkPerm()) { requestPerm(); return@setOnClickListener }
            val i = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
            Toast.makeText(this, "Seguimiento INICIADO - No se apaga hasta cerrar sesión", Toast.LENGTH_LONG).show()
        }

        btnDetener.setOnClickListener {
            // Este boton solo pausa, NO cierra sesion
            stopService(Intent(this, LocationService::class.java))
            Toast.makeText(this, "Seguimiento pausado (sesión sigue activa)", Toast.LENGTH_SHORT).show()
        }

        btnCerrarSesion.setOnClickListener {
            // UNICO lugar donde se cancela seguimiento
            stopService(Intent(this, LocationService::class.java))
            prefs.edit().clear().apply()
            Toast.makeText(this, "Sesión cerrada - Seguimiento cancelado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun checkPerm(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notif = if (Build.VERSION.SDK_INT >= 33) ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
        return fine && notif
    }
    private fun requestPerm() {
        val p = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, p.toTypedArray(), 1001)
    }
}
