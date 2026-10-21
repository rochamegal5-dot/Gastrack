package com.megal.gastrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var spinner: Spinner
    private lateinit var btnLogin: Button
    private lateinit var tvInfo: TextView
    private lateinit var progress: ProgressBar

    data class Repartidor(val nombre: String, val device_id: Int, val telefono: String)
    private var lista = mutableListOf<Repartidor>()
    private var seleccionado: Repartidor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        prefs = getSharedPreferences("gastrack", MODE_PRIVATE)

        // Si ya hay sesión iniciada, salta directo al mapa/tracking
        if (prefs.getBoolean("sesion_iniciada", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        spinner = findViewById(R.id.spinnerLogin)
        btnLogin = findViewById(R.id.btnIniciarSesion)
        tvInfo = findViewById(R.id.tvLoginInfo)
        progress = findViewById(R.id.progressLogin)

        cargarRepartidores()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (lista.isNotEmpty()) seleccionado = lista[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnLogin.setOnClickListener {
            if (seleccionado == null) {
                Toast.makeText(this, "Seleccioná un repartidor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // GUARDAR SESION - solo se borra con Cerrar Sesion
            prefs.edit().apply {
                putBoolean("sesion_iniciada", true)
                putString("deviceId", seleccionado!!.device_id.toString())
                putString("usuario", seleccionado!!.nombre)
                putString("telefono", seleccionado!!.telefono)
                putString("jornadaId", "ATODUR-${seleccionado!!.device_id}")
                apply()
            }
            Toast.makeText(this, "Bienvenido ${seleccionado!!.nombre}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun cargarRepartidores() {
        progress.visibility = View.VISIBLE
        tvInfo.text = "Cargando repartidores desde Turso..."
        thread {
            try {
                val url = URL("https://megalweb.vercel.app/api/repartidores")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val jsonStr = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(jsonStr)
                lista.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    lista.add(Repartidor(o.optString("nombre"), o.optInt("device_id"), o.optString("telefono")))
                }
                if (lista.isEmpty()) {
                    lista.add(Repartidor("Antonio", 7, ""))
                    lista.add(Repartidor("Jose", 8, ""))
                }
                runOnUiThread {
                    val nombres = lista.map { "${it.nombre} - Eq ${it.device_id}" }
                    spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
                    progress.visibility = View.GONE
                    tvInfo.text = "Seleccioná tu usuario"
                }
            } catch (e: Exception) {
                Log.e("ATODUR", "Error login: ${e.message}")
                runOnUiThread {
                    lista.clear()
                    lista.add(Repartidor("Antonio", 7, ""))
                    lista.add(Repartidor("Jose", 8, ""))
                    spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista.map { "${it.nombre} - Eq ${it.device_id}" })
                    progress.visibility = View.GONE
                    tvInfo.text = "Sin internet, lista local"
                }
            }
        }
    }
}
