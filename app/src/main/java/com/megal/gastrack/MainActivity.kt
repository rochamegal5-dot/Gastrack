
package com.megal.gastrack
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val prefsName = "gastrack_prefs"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "GasTrack Rocha\nCargando..." })
        // Simple UI programmatically to avoid layout XML issues
        val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(50,100,50,50) }
        val tv = TextView(this).apply { text = "GasTrack Rocha - TURSO\nSelecciona dispositivo:"; textSize = 18f }
        val spinner = Spinner(this)
        val devices = arrayOf("Antonio - device 7", "Jose - device 8")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, devices)
        val btn = Button(this).apply { text = "INICIAR TRACKING" }
        val status = TextView(this).apply { text = "Estado: detenido" }
        btn.setOnClickListener {
            val deviceId = if(spinner.selectedItemPosition==0) 7 else 8
            getSharedPreferences(prefsName, MODE_PRIVATE).edit().putInt("device_id", deviceId).apply()
            if(checkPermissions()) { startService(Intent(this, LocationService::class.java)); status.text="Enviando GPS device $deviceId a megalweb.vercel.app"; Toast.makeText(this,"Tracking iniciado",Toast.LENGTH_SHORT).show() } else { requestPermissions() }
        }
        layout.addView(tv); layout.addView(spinner); layout.addView(btn); layout.addView(status)
        setContentView(layout)
    }
    private fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val notif = if(Build.VERSION.SDK_INT>=33) ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED else true
        return fine && notif
    }
    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if(Build.VERSION.SDK_INT>=33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
    }
}
