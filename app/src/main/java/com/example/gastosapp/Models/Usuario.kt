package com.example.gastosapp.Models

data class Usuario(
    val nombre: String = "",
    val fotoUrl: String? = null,
    val proveedor: String = "Email",
    val creadoEn: Long = System.currentTimeMillis()
)