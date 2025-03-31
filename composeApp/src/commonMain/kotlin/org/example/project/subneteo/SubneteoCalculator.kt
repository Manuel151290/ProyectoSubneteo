package org.example.project.subneteo

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

object SubneteoCalculator {

    fun calcularPorHosts(clase: String, hosts: Int): SubneteoResultado {
        val bitsHost = ceil(ln((hosts + 2).toDouble()) / ln(2.0)).toInt()
        val bitsRed = 32 - bitsHost
        return calcularComun(clase, bitsRed)
    }

    fun calcularPorPrefijo(clase: String, prefijo: Int): SubneteoResultado {
        val bitsRed = prefijo
        return calcularComun(clase, bitsRed)
    }
    fun calcularPorIPyPrefijo(ip: String, prefijo: Int): SubneteoResultado {
        val ipPartes = ip.split(".").map { it.toInt() }

        if (ipPartes.size != 4 || ipPartes.any { it !in 0..255 } || prefijo !in 1..32)
            throw IllegalArgumentException("IP o prefijo inválido")

        val ipBinaria = ipPartes.joinToString("") { it.toString(2).padStart(8, '0') }
        val mascaraBinaria = "1".repeat(prefijo).padEnd(32, '0')
        val redBinaria = ipBinaria.zip(mascaraBinaria) { a, b -> if (a == '1' && b == '1') '1' else '0' }.joinToString("")
        val broadcastBinaria = ipBinaria.zip(mascaraBinaria) { a, b -> if (b == '1') a else '1' }.joinToString("")

        fun binToIp(bin: String) = List(4) { bin.substring(it * 8, it * 8 + 8).toInt(2) }.joinToString(".")

        val red = binToIp(redBinaria)
        val broadcast = binToIp(broadcastBinaria)

        val primeraIp = binToIp((redBinaria.take(31) + "1"))
        val ultimaIp = binToIp((broadcastBinaria.dropLast(1) + "0"))

        val cantidadHosts = (1 shl (32 - prefijo)) - 2
        val explicacion = """
        Se usó la IP: $ip y el prefijo /$prefijo
        
        La máscara en binario tiene $prefijo unos (1) y ${32 - prefijo} ceros (0).
        Aplicando operación lógica AND entre la IP y la máscara se obtuvo la dirección de red.
        
        Hosts válidos: 2^${32 - prefijo} - 2 = $cantidadHosts
        Dirección de red: todos los bits de host en 0
        Broadcast: todos los bits de host en 1
    """.trimIndent()

        return SubneteoResultado(
            direccionRed = red,
            direccionRedBin = redBinaria.chunked(8).joinToString("."),
            direccionBroadcast = broadcast,
            direccionBroadcastBin = broadcastBinaria.chunked(8).joinToString("."),
            ipInicial = primeraIp,
            ipFinal = ultimaIp,
            cantidadHosts = cantidadHosts,
            mascaraSubred = binToIp(mascaraBinaria),
            mascaraBinaria = mascaraBinaria.chunked(8).joinToString("."),
            explicacion = explicacion
        )
    }

    private fun calcularComun(clase: String, bitsRed: Int): SubneteoResultado {
        val bitsHost = 32 - bitsRed
        val mascaraBinaria = "1".repeat(bitsRed).padEnd(32, '0')
        val mascaraDecimal = mascaraBinaria.chunked(8).joinToString(".") { it.toInt(2).toString() }

        val direccionRed = when (clase.uppercase()) {
            "A" -> listOf(10, 0, 0, 0)
            "B" -> listOf(172, 16, 0, 0)
            "C" -> listOf(192, 168, 0, 0)
            else -> listOf(0, 0, 0, 0)
        }

        val direccionRedStr = direccionRed.joinToString(".")
        val direccionRedBin = direccionRed.joinToString(" ") { it.toString(2).padStart(8, '0') }

        val bloques = mascaraBinaria.chunked(8).map { it.toInt(2) }
        val broadcast = direccionRed.toMutableList()
        for (i in broadcast.indices) {
            broadcast[i] = direccionRed[i] or (255 - bloques[i])
        }

        val direccionBroadcast = broadcast.joinToString(".")
        val direccionBroadcastBin = broadcast.joinToString(" ") { it.toString(2).padStart(8, '0') }

        val ipInicial = direccionRed.toMutableList().apply { this[3] += 1 }.joinToString(".")
        val ipFinal = broadcast.toMutableList().apply { this[3] -= 1 }.joinToString(".")

        val cantidadHosts = (2.0.pow(bitsHost) - 2).toInt()

        val explicacion = """
            Bits de red: $bitsRed, bits de host: $bitsHost
            2^$bitsHost - 2 = $cantidadHosts hosts válidos
            Se excluye:
              - Dirección de red: todos los bits de host en 0
              - Dirección de broadcast: todos los bits de host en 1
        """.trimIndent()

        return SubneteoResultado(
            direccionRedStr,
            direccionRedBin,
            direccionBroadcast,
            direccionBroadcastBin,
            ipInicial,
            ipFinal,
            mascaraDecimal,
            mascaraBinaria.chunked(8).joinToString(" "),
            cantidadHosts,
            explicacion
        )
    }
}
