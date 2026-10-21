package com.megal.gastrack

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LocationService : Service() {
    private lateinit var fused: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        val notif = NotificationCompat.Builder(this, "track")
            .setContentTitle("ATODUR - Seguimiento activo")
            .setContentText("Registrando velocidad y paradas en Turso")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(1, notif)
        startTracking()
    }

    private fun startTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).setMinUpdateIntervalMillis(3000).build()
        try {
            fused.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    for (loc in r.locations) {
                        val speedKmh = loc.speed * 3.6f
                        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
                        val deviceId = prefs.getString("deviceId", "7")!!
                        thread {
                            try {
                                val json = JSONObject().put("device_id", deviceId).put("lat", loc.latitude).put("lng", loc.longitude).put("speed", speedKmh).put("accuracy", loc.accuracy)
                                val url = URL("https://megalweb.vercel.app/api/locations")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"; conn.setRequestProperty("Content-Type", "application/json"); conn.doOutput = true
                                conn.outputStream.write(json.toString().toByteArray())
                                conn.inputStream.close(); conn.disconnect()
                            } catch (e: Exception) {}
                        }
                    }
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {}
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("track", "Tracking ATODUR", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
