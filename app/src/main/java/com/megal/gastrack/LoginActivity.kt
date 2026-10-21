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
    data class Repartidor(val nombre: String, val device_id: Int)
    private var lista = mutableListOf<Repartidor>()
    private var seleccionado: Repartidor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_login)
            prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
            if (prefs.getBoolean("sesion_iniciada", false)) {
                startActivity(Intent(this, MainActivity::class.java)); finish(); return
            }
            spinner = findViewById(R.id.spinnerLogin)
            btnLogin = findViewById(R.id.btnIniciarSesion)
            tvInfo = findViewById(R.id.tvLoginInfo)
            progress = findViewById(R.id.progressLogin)
            cargar()
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (pos < lista.size) seleccionado = lista[pos]
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            btnLogin.setOnClickListener {
                if (seleccionado == null) { Toast.makeText(this,"Elegí repartidor",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                prefs.edit().putBoolean("sesion_iniciada",true).putString("deviceId", seleccionado!!.device_id.toString()).putString("usuario", seleccionado!!.nombre).putString("jornadaId","ATODUR-${seleccionado!!.device_id}").apply()
                startActivity(Intent(this, MainActivity::class.java)); finish()
            }
        } catch (e: Exception) {
            Log.e("LOGIN_CRASH", "Error onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun cargar() {
        progress.visibility = View.VISIBLE
        thread {
            try {
                val url = URL("https://megalweb.vercel.app/api/repartidores")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000; conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(text)
                lista.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    lista.add(Repartidor(o.optString("nombre","Rep"), o.optInt("device_id", i+7)))
                }
            } catch (e: Exception) {
                Log.e("LOGIN_CRASH","Turso fail ${e.message}")
            }
            if (lista.isEmpty()) {
                lista.add(Repartidor("Antonio", 7))
                lista.add(Repartidor("Jose", 8))
            }
            runOnUiThread {
                try {
                    progress.visibility = View.GONE
                    tvInfo.text = "Repartidores: ${lista.size}"
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista.map { "${it.nombre} - Eq ${it.device_id}" })
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                } catch (e: Exception) {
                    Log.e("LOGIN_CRASH","UI error", e)
                }
            }
        }
    }
}
