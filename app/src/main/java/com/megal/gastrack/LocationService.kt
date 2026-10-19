
package com.megal.gastrack
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
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
        val channel = NotificationChannel(channelId, "GasTrack v15 TURSO", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        val prefs = getSharedPreferences("gastrack", Context.MODE_PRIVATE)
        jornadaId = prefs.getString("jornadaId","test") ?: "test"
        usuario = prefs.getString("usuario","rocha") ?: "rocha"
        deviceId = prefs.getString("deviceId","7") ?: "7"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GasTrack v15 - En reparto")
            .setContentText("Equipo $deviceId - $usuario - $jornadaId -> Turso")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notification)

        fused = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        try{
            fused.requestLocationUpdates(request, object: LocationCallback(){
                override fun onLocationResult(r: LocationResult){
                    val loc = r.lastLocation ?: return
                    scope.launch { sendToTurso(loc) }
                }
            }, Looper.getMainLooper())
        }catch(e:SecurityException){}
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
                put("velocidad", (loc.speed * 3.6f))
            }.toString()
            conn.outputStream.write(json.toByteArray())
            val code = conn.responseCode
            println("TURSO SEND $code $json")
            conn.disconnect()
        }catch(e: Exception){ e.printStackTrace() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy(){ super.onDestroy(); scope.cancel() }
}
