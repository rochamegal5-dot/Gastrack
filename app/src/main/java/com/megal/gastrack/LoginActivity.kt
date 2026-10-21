package com.megal.gastrack

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.URL
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {
    data class Repartidor(val nombre: String, val device_id: Int)
    private var lista = mutableListOf<Repartidor>()
    private var seleccionado: Repartidor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        if (prefs.getBoolean("sesion_iniciada", false)) {
            startActivity(Intent(this, MainActivity::class.java)); finish(); return
        }

        val spinner = findViewById<Spinner>(R.id.spinnerLogin)
        val tvInfo = findViewById<TextView>(R.id.tvLoginInfo)
        val btn = findViewById<Button>(R.id.btnIniciarSesion)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                if (pos < lista.size) seleccionado = lista[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Carga de Turso desde megalweb
        thread {
            try {
                val text = URL("https://megalweb.vercel.app/api/repartidores").readText()
                val arr = JSONArray(text)
                lista.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    // Soporta device_id o deviceId
                    val id = if (o.has("device_id")) o.getInt("device_id") else o.getInt("deviceId")
                    lista.add(Repartidor(o.getString("nombre"), id))
                }
            } catch (e: Exception) { e.printStackTrace() }
            if (lista.isEmpty()) {
                lista.add(Repartidor("Antonio - Equipo 7", 7))
                lista.add(Repartidor("Jose - Equipo 8", 8))
            }
            runOnUiThread {
                tvInfo.text = "Turso: ${lista.size} repartidores"
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista.map { "${it.nombre}" })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
        }

        btn.setOnClickListener {
            if (seleccionado == null) { Toast.makeText(this, "Seleccioná repartidor", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            prefs.edit().putBoolean("sesion_iniciada", true).putString("deviceId", seleccionado!!.device_id.toString()).putString("usuario", seleccionado!!.nombre).apply()
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
    }
}
