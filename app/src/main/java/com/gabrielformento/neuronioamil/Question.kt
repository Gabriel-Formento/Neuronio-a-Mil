package com.gabrielformento.neuronioamil

data class Question(
    val texto: String,
    val opcoes: List<String>,
    val correta: Int,
    val nivel: String,
    val categoria: String,
    val audio: String? = null
)