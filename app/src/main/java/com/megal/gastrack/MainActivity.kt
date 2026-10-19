
package com.megal.gastrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity: AppCompatActivity(){
    private lateinit var txtStatus: TextView
    private lateinit var edtUsuario: EditText
    private lateinit var edtDevice: EditText
    private lateinit var btnJornada: Button

    private var enJornada = false
    private var jornadaId = ""

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,80,40,40) }
        val title = TextView(this).apply { text = "GasTrack v1.0.15-TURSO\nBase: megal-erp"; textSize = 20f }
        edtUsuario = EditText(this).apply { hint = "Usuario (ej: Megal Rocha)"; setText("Megal Rocha") }
        edtDevice = EditText(this).apply { hint = "Equipo ID (7 u 8)"; setText("7"); inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        txtStatus = TextView(this).apply { text = "Listo para iniciar jornada"; textSize = 16f; setPadding(0,30,0,30) }
        btnJornada = Button(this).apply { text = "INICIAR JORNADA" }
        val btnStop = Button(this).apply { text = "DETENER JORNADA" }

        btnJornada.setOnClickListener { iniciarJornada() }
        btnStop.setOnClickListener { detenerJornada() }

        layout.addView(title); layout.addView(edtUsuario); layout.addView(edtDevice); layout.addView(txtStatus); layout.addView(btnJornada); layout.addView(btnStop)
        setContentView(layout)

        checkPerms()
        // recuperar prefs
        val prefs = getSharedPreferences("gastrack", Context.MODE_PRIVATE)
        enJornada = prefs.getBoolean("enJornada", false)
        jornadaId = prefs.getString("jornadaId","") ?: ""
        if(enJornada){ txtStatus.text = "En jornada: $jornadaId"; btnJornada.text = "EN JORNADA" }
    }

    private fun checkPerms(){
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if(Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = perms.filter { ActivityCompat.checkSelfPermission(this,it) != PackageManager.PERMISSION_GRANTED }
        if(missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }

    private fun iniciarJornada(){
        val usuario = edtUsuario.text.toString().ifBlank { "rocha" }
        val device = edtDevice.text.toString().ifBlank { "7" }
        jornadaId = UUID.randomUUID().toString().substring(0,8)
        val prefs = getSharedPreferences("gastrack", Context.MODE_PRIVATE)
        prefs.edit().putString("usuario", usuario).putString("deviceId", device).putString("jornadaId", jornadaId).putBoolean("enJornada", true).apply()
        val intent = Intent(this, LocationService::class.java)
        if(Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        txtStatus.text = "Jornada iniciada: $jornadaId\nEquipo $device - $usuario\nEnviando a Turso megal-erp/gps_locations"
        btnJornada.text = "EN JORNADA: $jornadaId"
        Toast.makeText(this, "Jornada $jornadaId iniciada", Toast.LENGTH_LONG).show()
    }

    private fun detenerJornada(){
        stopService(Intent(this, LocationService::class.java))
        getSharedPreferences("gastrack", Context.MODE_PRIVATE).edit().putBoolean("enJornada", false).apply()
        txtStatus.text = "Jornada detenida"
        btnJornada.text = "INICIAR JORNADA"
        Toast.makeText(this, "Jornada detenida", Toast.LENGTH_SHORT).show()
    }
}
