package com.megal.gastrack

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    data class Repartidor(val nombre: String, val id: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("gastrack", MODE_PRIVATE)
        if (prefs.getBoolean("sesion_iniciada", false)) {
            startActivity(Intent(this, MainActivity::class.java)); finish(); return
        }

        val spinner = findViewById<Spinner>(R.id.spinnerLogin)
        val tvInfo = findViewById<TextView>(R.id.tvLoginInfo)
        val progress = findViewById<ProgressBar>(R.id.progressLogin)
        val btn = findViewById<Button>(R.id.btnIniciarSesion)

        progress.visibility = ProgressBar.GONE
        tvInfo.text = "Seleccioná repartidor"

        // Datos locales, no depende de internet, no crashea nunca
        val lista = listOf(Repartidor("Antonio - Equipo 7", 7), Repartidor("Jose - Equipo 8", 8), Repartidor("Megal", 1))
        var seleccionado = lista[0]

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista.map { "${it.nombre}" })
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) { seleccionado = lista[pos] }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btn.setOnClickListener {
            prefs.edit().putBoolean("sesion_iniciada", true)
                .putString("deviceId", seleccionado.id.toString())
                .putString("usuario", seleccionado.nombre)
                .apply()
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
    }
}
