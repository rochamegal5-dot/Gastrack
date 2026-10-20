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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var jornadaId = "ATODUR"
    private var usuario = "Antonio"
    private var deviceId = "7"
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        jornadaId = prefs.getString("jornadaId", "ATODUR") ?: "ATODUR"
        usuario = prefs.getString("usuario", "Antonio") ?: "Antonio"
        deviceId = prefs.getString("deviceId", "7") ?: "7"

        val channelId = "gastrack_channel"
        val channel = NotificationChannel(channelId, "ATODUR v1.57 FORZADO", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        fused = LocationServices.getFusedLocationProviderClient(this)
        
        // JOB que pide ubicación FRESCA cada 3 segundos - no usa cache
        job = scope.launch {
            while (isActive) {
                try {
                    // Fuerza GPS fresco, ignora cache
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                Log.d("ATODUR", "GPS FRESCO: ${loc.latitude}, ${loc.longitude} acc=${loc.accuracy}")
                                scope.launch { sendToTurso(loc) }
                                updateNotification(loc)
                            } else {
                                Log.d("ATODUR", "GPS null - sin señal")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ATODUR", "GPS fail: ${e.message}")
                        }
                } catch (e: Exception) {
                    Log.e("ATODUR", "Error getCurrent: ${e.message}")
                }
                delay(3000L)
            }
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ATODUR v1.57 - FORZADO 3s")
            .setContentText("Equipo $deviceId - Buscando GPS fresco...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build()
        startForeground(1, notification)
    }

    private fun updateNotification(loc: android.location.Location) {
        val channelId = "gastrack_channel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ATODUR v1.57 - ${loc.latitude.toString().take(8)}, ${loc.longitude.toString().take(8)}")
            .setContentText("Equipo $deviceId - acc ${loc.accuracy.toInt()}m - ${System.currentTimeMillis()/1000}")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification)
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

            Log.d("ATODUR", "POST $json")
            conn.outputStream.use { it.write(json.toByteArray()) }
            val code = conn.responseCode
            val resp = try { conn.inputStream.bufferedReader().readText() } catch (_: Exception) { conn.errorStream?.bufferedReader()?.readText() ?: "" }
            Log.d("ATODUR", "TURSO $code -> $resp")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("ATODUR", "Error Turso: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); job?.cancel(); scope.cancel() }
}
