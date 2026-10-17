
package com.megal.gastrack
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class LocationService : Service() {
    private lateinit var fused: FusedLocationProviderClient
    private val scope = CoroutineScope(Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        val notification = if(Build.VERSION.SDK_INT>=26) Notification.Builder(this, "gastrack").setContentTitle("GasTrack Rocha").setContentText("Enviando GPS a Turso").setSmallIcon(android.R.drawable.ic_menu_mylocation).build() else Notification.Builder(this).setContentTitle("GasTrack").build()
        startForeground(1, notification)
        startLocation()
    }
    private fun createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            val ch = NotificationChannel("gastrack","GasTrack",NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }
    private fun startLocation() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).setMinUpdateIntervalMillis(5000L).build()
        try {
            fused.requestLocationUpdates(req, object: LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    val loc = r.lastLocation ?: return
                    val prefs = getSharedPreferences("gastrack_prefs", MODE_PRIVATE)
                    val deviceId = prefs.getInt("device_id", 7)
                    scope.launch { sendToTurso(loc.latitude, loc.longitude, loc.speed.toDouble(), deviceId) }
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {}
    }
    private suspend fun sendToTurso(lat: Double, lon: Double, speed: Double, deviceId: Int) {
        try {
            val url = URL("https://megalweb.vercel.app/api/gps")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type","application/json")
            conn.doOutput = true
            val body = """{"device_id":$deviceId,"lat":$lat,"lng":$lon,"speed":$speed,"timestamp":${System.currentTimeMillis()}}"""
            conn.outputStream.write(body.toByteArray())
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {}
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
