package com.megal.gastrack

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var spinnerRepartidores: Spinner
    private lateinit var etJornada: EditText
    private lateinit var btnIniciar: Button
    private lateinit var btnDetener: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    // Lista que viene de Turso
    private var listaRepartidores = mutableListOf<Repartidor>()
    private var repartidorSeleccionado: Repartidor? = null

    data class Repartidor(val nombre: String, val device_id: Int, val telefono: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("gastrack", MODE_PRIVATE)

        spinnerRepartidores = findViewById(R.id.spinnerRepartidores)
        etJornada = findViewById(R.id.etJornada)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        // Cargar datos guardados
        etJornada.setText(prefs.getString("jornadaId", "ATODUR"))

        // Cargar repartidores desde Turso (Paso 1 que hicimos en la web)
        cargarRepartidoresDesdeTurso()

        spinnerRepartidores.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (listaRepartidores.isNotEmpty()) {
                    repartidorSeleccionado = listaRepartidores[pos]
                    tvStatus.text = "Seleccionado: ${repartidorSeleccionado!!.nombre} - Equipo ${repartidorSeleccionado!!.device_id}"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnIniciar.setOnClickListener {
            if (repartidorSeleccionado == null) {
                Toast.makeText(this, "Esperá que cargue la lista de repartidores", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (etJornada.text.toString().isBlank()) {
                Toast.makeText(this, "Poné Jornada ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!checkPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }
            guardarYIniciar()
        }

        btnDetener.setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            tvStatus.text = "Detenido"
            Toast.makeText(this, "Seguimiento detenido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarRepartidoresDesdeTurso() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Cargando repartidores desde Turso..."

        thread {
            try {
                val url = URL("https://megalweb.vercel.app/api/repartidores")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"

                val code = conn.responseCode
                val jsonStr = conn.inputStream.bufferedReader().readText()
                Log.d("ATODUR", "Repartidores API $code -> $jsonStr")

                val arr = JSONArray(jsonStr)
                listaRepartidores.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val nombre = obj.optString("nombre", "Sin nombre")
                    val deviceId = obj.optInt("device_id", 0)
                    val telefono = obj.optString("telefono", "")
                    if (deviceId != 0) {
                        listaRepartidores.add(Repartidor(nombre, deviceId, telefono))
                    }
                }

                // Si la tabla está vacía, agrega los por defecto para no quedar trabado
                if (listaRepartidores.isEmpty()) {
                    listaRepartidores.add(Repartidor("Antonio", 7, ""))
                    listaRepartidores.add(Repartidor("Jose", 8, ""))
                }

                runOnUiThread {
                    val nombres = listaRepartidores.map { "${it.nombre} - Equipo ${it.device_id}" }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
                    spinnerRepartidores.adapter = adapter

                    // Restaurar selección guardada
                    val savedDeviceId = prefs.getString("deviceId", "7")?.toIntOrNull() ?: 7
                    val index = listaRepartidores.indexOfFirst { it.device_id == savedDeviceId }
                    if (index >= 0) spinnerRepartidores.setSelection(index)

                    progressBar.visibility = View.GONE
                    tvStatus.text = "Repartidores cargados: ${listaRepartidores.size}"
                }

            } catch (e: Exception) {
                Log.e("ATODUR", "Error cargando repartidores: ${e.message}", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvStatus.text = "Error Turso, usando lista local"
                    // Fallback local
                    listaRepartidores.clear()
                    listaRepartidores.add(Repartidor("Antonio", 7, ""))
                    listaRepartidores.add(Repartidor("Jose", 8, ""))
                    val nombres = listaRepartidores.map { "${it.nombre} - Equipo ${it.device_id}" }
                    spinnerRepartidores.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
                }
            }
        }
    }

    private fun guardarYIniciar() {
        val rep = repartidorSeleccionado ?: return

        prefs.edit().apply {
            putString("deviceId", rep.device_id.toString())
            putString("usuario", rep.nombre)
            putString("jornadaId", etJornada.text.toString().trim())
            apply()
        }

        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        tvStatus.text = "INICIADO: ${rep.nombre} (Equipo ${rep.device_id}) -> ${etJornada.text}"
        Toast.makeText(this, "Iniciado ${rep.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notif = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return fine && coarse && notif
    }

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            guardarYIniciar()
        } else {
            Toast.makeText(this, "Dale permiso de ubicación SIEMPRE y notificaciones", Toast.LENGTH_LONG).show()
        }
    }
}
