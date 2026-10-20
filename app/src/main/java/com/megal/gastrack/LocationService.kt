package com.megal.gastrack
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.BatteryManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class LocationService: Service(){
    private lateinit var fused: FusedLocationProviderClient
    private val scope = CoroutineScope(Dispatchers.IO)
    private var jornadaId: String = ""
    private var usuario: String = ""
    private var deviceId: String = "7"

    override fun onCreate(){
        super.onCreate()
        val channelId = "gastrack_channel"
        val channel = NotificationChannel(channelId, "GasTrack ATODUR", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        val prefs = getSharedPreferences("gastrack", Context.MODE_PRIVATE)
        jornadaId = prefs.getString("jornadaId","test") ?: "test"
        usuario = prefs.getString("usuario","rocha") ?: "rocha"
        deviceId = prefs.getString("deviceId","7") ?: "7"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ATODUR - Tracking activo")
            .setContentText("Equipo $deviceId - $usuario - $jornadaId -> megalweb.vercel.app")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        startForeground(1, notification)

        fused = LocationServices.getFusedLocationProviderClient(this)
        // FIX: 3000ms + min 2000ms = Life360 ATODUR
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(true)
        }.build()
        try{
            fused.requestLocationUpdates(request, object: LocationCallback(){
                override fun onLocationResult(r: LocationResult){
                    val loc = r.lastLocation ?: return
                    updateNotif("${loc.latitude}, ${loc.longitude} | ${String.format("%.1f km/h", loc.speed*3.6)}")
                    scope.launch { sendToTurso(loc) }
                }
            }, Looper.getMainLooper())
        }catch(e:SecurityException){}
    }

    private fun updateNotif(text: String) {
        val channelId = "gastrack_channel"
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ATODUR - Tracking activo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, notif)
    }

    private fun getBattery(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun sendToTurso(loc: Location){
        try{
            val url = URL("https://megalweb.vercel.app/api/ingest")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod="POST"
            conn.setRequestProperty("Content-Type","application/json")
            conn.doOutput=true
            val json = JSONObject().apply {
                put("device_id", deviceId.toIntOrNull() ?: 7)
                put("usuario", usuario)
                put("jornada_id", jornadaId)
                put("lat", loc.latitude)
                put("lng", loc.longitude)
                put("accuracy", loc.accuracy)
                put("velocidad", (loc.speed * 3.6f))
                put("speed", loc.speed)
                put("battery", getBattery())
                put("timestamp", System.currentTimeMillis())
            }.toString()
            conn.outputStream.write(json.toByteArray())
            val code = conn.responseCode
            println("MEGALWEB SEND $code $json -> ${conn.inputStream.bufferedReader().readText().take(200)}")
            conn.disconnect()
        }catch(e: Exception){ e.printStackTrace() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy(){ super.onDestroy(); scope.cancel() }
}
