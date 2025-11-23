package com.example.gastosapp.Models

enum class Categoria(val nombre: String) {
    ALIMENTACION("Alimentación"),
    TRANSPORTE("Transporte"),
    ENTRETENIMIENTO("Entretenimiento"),
    SALUD("Salud"),
    EDUCACION("Educación"),
    COMPRAS("Compras"),
    HOGAR("Hogar"),
    OTROS("Otros");

    companion object {
        fun fromNombre(nombre: String): Categoria =
            values().find { it.nombre == nombre } ?: OTROS
    }
}