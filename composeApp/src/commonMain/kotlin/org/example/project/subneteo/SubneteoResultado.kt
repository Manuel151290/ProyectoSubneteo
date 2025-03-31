package org.example.project.subneteo

data class SubneteoResultado(
    val direccionRed: String,
    val direccionRedBin: String,
    val direccionBroadcast: String,
    val direccionBroadcastBin: String,
    val ipInicial: String,
    val ipFinal: String,
    val mascaraSubred: String,
    val mascaraBinaria: String,
    val cantidadHosts: Int,
    val explicacion: String
)
