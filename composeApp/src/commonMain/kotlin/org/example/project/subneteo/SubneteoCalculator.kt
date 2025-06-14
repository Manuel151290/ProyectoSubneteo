package org.example.project.subneteo

// Importaciones necesarias para operaciones matemáticas
import kotlin.math.ceil // Para redondear hacia arriba
import kotlin.math.ln   // Para logaritmo natural
import kotlin.math.pow  // Para potencias

// Objeto singleton que contiene las funciones de cálculo de subredes
object SubneteoCalculator {

    // Calcula la subred a partir de la clase y la cantidad de hosts requeridos
    fun calcularPorHosts(clase: String, hosts: Int): SubneteoResultado {
        // Calcula los bits necesarios para los hosts (incluye red y broadcast)
        val bitsHost = ceil(ln((hosts + 2).toDouble()) / ln(2.0)).toInt()
        // Calcula los bits de red restantes
        val bitsRed = 32 - bitsHost
        // Llama a la función común para obtener el resultado
        return calcularComun(clase, bitsRed)
    }

    // Calcula la subred a partir de la clase y el prefijo (CIDR)
    fun calcularPorPrefijo(clase: String, prefijo: Int): SubneteoResultado {
        val bitsRed = prefijo // El prefijo indica directamente los bits de red
        return calcularComun(clase, bitsRed)
    }

    // Calcula la subred a partir de una IP y un prefijo (CIDR)
    fun calcularPorIPyPrefijo(ip: String, prefijo: Int): SubneteoResultado {
        // Divide la IP en partes y las convierte a enteros
        val ipPartes = ip.split(".").map { it.toInt() }

        // Valida que la IP y el prefijo sean correctos
        if (ipPartes.size != 4 || ipPartes.any { it !in 0..255 } || prefijo !in 1..32)
            throw IllegalArgumentException("IP o prefijo inválido")

        // Convierte la IP a binario (32 bits)
        val ipBinaria = ipPartes.joinToString("") { it.toString(2).padStart(8, '0') }
        // Crea la máscara en binario
        val mascaraBinaria = "1".repeat(prefijo).padEnd(32, '0')
        // Calcula la red aplicando AND entre IP y máscara
        val redBinaria = ipBinaria.zip(mascaraBinaria) { a, b -> if (a == '1' && b == '1') '1' else '0' }.joinToString("")
        // Calcula el broadcast poniendo los bits de host en 1
        val broadcastBinaria = ipBinaria.zip(mascaraBinaria) { a, b -> if (b == '1') a else '1' }.joinToString("")

        // Función auxiliar para convertir binario a IP decimal
        fun binToIp(bin: String) = List(4) { bin.substring(it * 8, it * 8 + 8).toInt(2) }.joinToString(".")

        // Obtiene la dirección de red y broadcast en decimal
        val red = binToIp(redBinaria)
        val broadcast = binToIp(broadcastBinaria)

        // Calcula la primera y última IP válida
        val primeraIp = binToIp((redBinaria.take(31) + "1"))
        val ultimaIp = binToIp((broadcastBinaria.dropLast(1) + "0"))

        // Calcula la cantidad de hosts válidos
        val cantidadHosts = (1 shl (32 - prefijo)) - 2
        // Explicación detallada del cálculo
        val explicacion = """
        Se usó la IP: $ip y el prefijo /$prefijo
        
        La máscara en binario tiene $prefijo unos (1) y ${32 - prefijo} ceros (0).
        Aplicando operación lógica AND entre la IP y la máscara se obtuvo la dirección de red.
        
        Hosts válidos: 2^${32 - prefijo} - 2 = $cantidadHosts
        Dirección de red: todos los bits de host en 0
        Broadcast: todos los bits de host en 1
    """.trimIndent()

        // Devuelve el resultado con todos los datos calculados
        return SubneteoResultado(
            direccionRed = red, // Dirección de red en decimal
            direccionRedBin = redBinaria.chunked(8).joinToString("."), // Dirección de red en binario
            direccionBroadcast = broadcast, // Dirección de broadcast en decimal
            direccionBroadcastBin = broadcastBinaria.chunked(8).joinToString("."), // Broadcast en binario
            ipInicial = primeraIp, // Primera IP válida
            ipFinal = ultimaIp, // Última IP válida
            cantidadHosts = cantidadHosts, // Cantidad de hosts válidos
            mascaraSubred = binToIp(mascaraBinaria), // Máscara en decimal
            mascaraBinaria = mascaraBinaria.chunked(8).joinToString("."), // Máscara en binario
            explicacion = explicacion // Explicación del cálculo
        )
    }

    // Función común para calcular subredes a partir de clase y bits de red
    private fun calcularComun(clase: String, bitsRed: Int): SubneteoResultado {
        val bitsHost = 32 - bitsRed // Calcula los bits de host
        val mascaraBinaria = "1".repeat(bitsRed).padEnd(32, '0') // Máscara en binario
        val mascaraDecimal = mascaraBinaria.chunked(8).joinToString(".") { it.toInt(2).toString() } // Máscara en decimal

        // Asigna la IP base según la clase
        val direccionRed = when (clase.uppercase()) {
            "A" -> listOf(10, 0, 0, 0)
            "B" -> listOf(172, 16, 0, 0)
            "C" -> listOf(192, 168, 0, 0)
            else -> listOf(0, 0, 0, 0)
        }

        val direccionRedStr = direccionRed.joinToString(".") // Dirección de red en string
        val direccionRedBin = direccionRed.joinToString(" ") { it.toString(2).padStart(8, '0') } // Red en binario

        // Calcula los bloques de la máscara
        val bloques = mascaraBinaria.chunked(8).map { it.toInt(2) }
        val broadcast = direccionRed.toMutableList()
        // Calcula la dirección de broadcast sumando los bits de host en 1
        for (i in broadcast.indices) {
            broadcast[i] = direccionRed[i] or (255 - bloques[i])
        }

        val direccionBroadcast = broadcast.joinToString(".") // Broadcast en decimal
        val direccionBroadcastBin = broadcast.joinToString(" ") { it.toString(2).padStart(8, '0') } // Broadcast en binario

        // Calcula la primera y última IP válida
        val ipInicial = direccionRed.toMutableList().apply { this[3] += 1 }.joinToString(".")
        val ipFinal = broadcast.toMutableList().apply { this[3] -= 1 }.joinToString(".")

        // Calcula la cantidad de hosts válidos
        val cantidadHosts = (2.0.pow(bitsHost) - 2).toInt()

        // Explicación del cálculo
        val explicacion = """
            Bits de red: $bitsRed, bits de host: $bitsHost
            2^$bitsHost - 2 = $cantidadHosts hosts válidos
            Se excluye:
              - Dirección de red: todos los bits de host en 0
              - Dirección de broadcast: todos los bits de host en 1
        """.trimIndent()

        // Devuelve el resultado con todos los datos calculados
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
