package com.gabrielformento.neuronioamil

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Recorde(val nome: String, val pontos: Int)

class StatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stats_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var acertosTotais: Int
        get() = prefs.getInt("acertos", 0)
        set(value) = prefs.edit().putInt("acertos", value).apply()

    var errosTotais: Int
        get() = prefs.getInt("erros", 0)
        set(value) = prefs.edit().putInt("erros", value).apply()

    var partidasJogadas: Int
        get() = prefs.getInt("partidas", 0)
        set(value) = prefs.edit().putInt("partidas", value).apply()

    var temaAtual: String
        get() = prefs.getString("tema", "azul") ?: "azul"
        set(value) = prefs.edit().putString("tema", value).apply()

    // Controle de Som Global
    var somAtivo: Boolean
        get() = prefs.getBoolean("som_ativo", true)
        set(value) = prefs.edit().putBoolean("som_ativo", value).apply()

    fun getPerguntasVistas(): MutableList<String> {
        val json = prefs.getString("vistas_auto", "[]")
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun salvarPerguntaVista(texto: String) {
        val lista = getPerguntasVistas()
        lista.add(texto)
        prefs.edit().putString("vistas_auto", gson.toJson(lista)).apply()
    }

    fun setPerguntasVistas(novaLista: MutableList<String>) {
        prefs.edit().putString("vistas_auto", gson.toJson(novaLista)).apply()
    }

    fun getTop5(): List<Recorde> {
        val json = prefs.getString("recordes", "[]")
        val type = object : TypeToken<List<Recorde>>() {}.type
        return gson.fromJson(json, type)
    }

    fun salvarRecorde(novo: Recorde) {
        val lista = getTop5().toMutableList()
        lista.add(novo)
        lista.sortByDescending { it.pontos }
        val top5 = lista.take(5)
        prefs.edit().putString("recordes", gson.toJson(top5)).apply()
    }

    fun temConquista(id: Int): Boolean = prefs.getBoolean("c$id", false)

    fun ganharConquista(id: Int) {
        prefs.edit().putBoolean("c$id", true).apply()
    }
}