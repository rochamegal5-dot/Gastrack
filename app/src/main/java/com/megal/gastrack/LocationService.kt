package com.megal.gastrack

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LocationService : Service() {
    private lateinit var fused: FusedLocationProviderClient
    private val scope = CoroutineScope(Dispatchers.IO)
    private var jornadaId = "ATODUR"
    private var usuario = "Antonio"
    private var deviceId = "7"

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        jornadaId = prefs.getString("jornadaId", "ATODUR") ?: "ATODUR"
        usuario = prefs.getString("usuario", "Antonio") ?: "Antonio"
        deviceId = prefs.getString("deviceId", "7") ?: "7"

        val channelId = "gastrack_channel"
        val channel = NotificationChannel(channelId, "ATODUR v1.57 TURSO", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ATODUR v1.57 - TURSO LIVE 3s")
            .setContentText("Equipo $deviceId -> /api/ingest")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build()
        startForeground(1, notification)

        fused = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
            setMinUpdateIntervalMillis(2000L)
        }.build()

        try {
            fused.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    val loc = r.lastLocation ?: return
                    scope.launch { sendToTurso(loc) }
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("ATODUR", "Sin permiso: ${e.message}")
        }
    }

    private fun getBattery(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun sendToTurso(loc: android.location.Location) {
        try {
            val url = URL("https://megalweb.vercel.app/api/ingest")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val json = JSONObject().apply {
                put("device_id", deviceId.toIntOrNull() ?: 7)
                put("usuario", usuario)
                put("jornada_id", jornadaId)
                put("lat", loc.latitude)
                put("lng", loc.longitude)
                put("accuracy", loc.accuracy)
                put("velocidad", loc.speed * 3.6f)
                put("battery", getBattery())
            }.toString()

            Log.d("ATODUR", "Enviando: $json")
            conn.outputStream.use { it.write(json.toByteArray()) }
            val code = conn.responseCode
            val response = conn.inputStream.bufferedReader().readText()
            Log.d("ATODUR", "TURSO $code -> $response")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("ATODUR", "Error Turso: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
