// App.kt COMPLETO Y CORREGIDO CON OPCIÓN DE IP + PREFIJO
package org.example.project

// Importaciones de Jetpack Compose para UI multiplataforma
import androidx.compose.foundation.horizontalScroll // Para scroll horizontal en componentes
import androidx.compose.foundation.layout.* // Para layouts y espaciado
import androidx.compose.foundation.rememberScrollState // Para recordar el estado de scroll
import androidx.compose.foundation.verticalScroll // Para scroll vertical
import androidx.compose.material.* // Componentes de Material Design
import androidx.compose.runtime.* // Para estados y composición reactiva
import androidx.compose.ui.Alignment // Para alineación de elementos
import androidx.compose.ui.Modifier // Para modificar el comportamiento de los componentes
import androidx.compose.ui.text.input.TextFieldValue // Para manejar el texto de los campos de entrada
import androidx.compose.ui.unit.dp // Para definir dimensiones en dp
import kotlinx.coroutines.launch // Para lanzar corrutinas
import org.example.project.subneteo.SubneteoCalculator // Importa la lógica de subneteo
import org.example.project.subneteo.SubneteoResultado // Importa el modelo de resultado
import kotlin.math.pow //
import androidx.compose.ui.unit.sp


@Composable // Indica que esta función es una función composable de Jetpack Compose
fun App() {
    MaterialTheme { // Aplica el tema de Material Design
        val scaffoldState = rememberScaffoldState() // Estado del Scaffold (snackbar, drawer, etc)
        val coroutineScope = rememberCoroutineScope() // Ámbito para lanzar corrutinas

        // Estados para controlar el modo de cálculo y los valores de entrada
        var modoPorHosts by remember { mutableStateOf(true) } // Si está activo el modo por hosts
        var modoPorIP by remember { mutableStateOf(false) } // Si está activo el modo por IP/prefijo
        var clase by remember { mutableStateOf("A") } // Clase de IP seleccionada
        var valor by remember { mutableStateOf(TextFieldValue("")) } // Valor de hosts o prefijo
        var ipTexto by remember { mutableStateOf(TextFieldValue("")) } // Texto de la IP ingresada
        var prefijoTexto by remember { mutableStateOf(TextFieldValue("")) } // Texto del prefijo ingresado
        var resultado by remember { mutableStateOf<SubneteoResultado?>(null) } // Resultado del cálculo
        var explicacionPopup by remember { mutableStateOf(false) } // Controla si se muestra el popup de explicación
        var leyendaPopup by remember { mutableStateOf(false) } // Controla si se muestra el popup de leyenda
        var calculadoraSubredesPopup by remember { mutableStateOf(false) } // Controla si se muestra el popup de calculadora de subredes
        var conversorPopup by remember { mutableStateOf(false) } // Controla si se muestra el popup del conversor
        val modoSugerencia = remember { mutableStateOf(false) }
        var cantidadIPs by remember { mutableStateOf("") }
        var sugerenciaResultado by remember { mutableStateOf<String?>(null) }
        var sugerenciaExplicacion by remember { mutableStateOf("") }

        // Estados para scroll en la UI
        val verticalScroll = rememberScrollState() // Estado de scroll vertical
        val horizontalScroll = rememberScrollState() // Estado de scroll horizontal

        Scaffold(scaffoldState = scaffoldState) { paddingValues -> // Estructura base de la pantalla
            Column(
                modifier = Modifier
                    .fillMaxSize() // Ocupa todo el tamaño disponible
                    .verticalScroll(verticalScroll) // Permite scroll vertical
                    .padding(paddingValues) // Padding del Scaffold
                    .padding(16.dp), // Padding adicional
                horizontalAlignment = Alignment.Start // Alinea los hijos al inicio horizontal
            ) {
                Text("🔧 Calculadora de Subneteo", style = MaterialTheme.typography.h6) // Título principal
                Spacer(Modifier.height(8.dp)) // Espacio vertical

                Column {
                    Text("Clase de IP (elige el rango base para calcular el subneteo):") // Instrucción
                    Spacer(Modifier.height(4.dp)) // Espacio vertical
                    DropdownMenuSelector(current = clase, options = listOf("A", "B", "C", "D", "E")) {
                        // Selector desplegable para elegir la clase de IP (A, B, C, D, E)
                        // Cuando el usuario selecciona una clase:
                        if (it in listOf("D", "E")) { // Si la clase es D o E (no válidas para subneteo)
                            coroutineScope.launch {
                                // Muestra un mensaje informativo en la parte inferior de la pantalla
                                scaffoldState.snackbarHostState.showSnackbar(
                                    when (it) {
                                        "D" -> "❌ Clase D no disponible: es para multicast, no para direccionamiento de dispositivos."
                                        "E" -> "❌ Clase E reservada: se usa para investigación y pruebas."
                                        else -> "❌ Clase $it no permitida para subneteo."
                                    }
                                )
                            }
                        } else {
                            // Si la clase es válida (A, B o C), la asigna como seleccionada
                            clase = it
                        }
                    }
                    Spacer(Modifier.height(8.dp)) // Espacio vertical entre el selector y el botón
                    Button(onClick = { leyendaPopup = true }) { // Botón para mostrar la leyenda de clases IP
                        Text("ℹ️ Ver leyenda") // Texto del botón
                    }
                    Spacer(Modifier.width(8.dp)) // Espacio horizontal
                    Button(onClick = { calculadoraSubredesPopup = true }) { // Botón para mostrar la calculadora de subredes
                        Text("🧮 Calculadora de subredes")
                    }
                    Spacer(Modifier.width(8.dp)) // Espacio horizontal
                    Button(onClick = { conversorPopup = true }) { // Botón para mostrar el conversor decimal/binario
                        Text("🔢 Conversor decimal/binario")
                    }
                }

                Spacer(Modifier.height(8.dp)) // Espacio vertical entre secciones

                Column {
                    // Opción para calcular por número de hosts
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modoPorHosts, onClick = {
                            modoPorHosts = true; modoPorIP = false; modoSugerencia.value = false
                        })
                        Text("Por número de hosts") // Etiqueta de la opción
                    }
                    // Opción para calcular por prefijo CIDR
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !modoPorHosts && !modoPorIP && !modoSugerencia.value, onClick = {
                            modoPorHosts = false; modoPorIP = false; modoSugerencia.value = false
                        })
                        Text("Por prefijo CIDR") // Etiqueta de la opción
                    }
                    // Opción para calcular por dirección IP y prefijo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modoPorIP, onClick = {
                            modoPorIP = true; modoPorHosts = false; modoSugerencia.value = false
                        })
                        Text("Por dirección IP + prefijo") // Etiqueta de la opción
                    }
                    // Opción para sugerir clase y rango de IP según la cantidad de IPs requeridas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { modoSugerencia.value = true }) {
                            Text("Sugerir clase de red por cantidad de dispositivos")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp)) // Espacio vertical entre secciones

                if (!modoPorIP && !modoSugerencia.value) {
                    TextField(
                        value = valor,
                        onValueChange = { valor = it },
                        label = {
                            Text(if (modoPorHosts) "Número de hosts (dispositivos requeridos)" else "Prefijo CIDR (ej. 18)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (modoPorIP) {
                    TextField(
                        value = ipTexto,
                        onValueChange = { ipTexto = it },
                        label = { Text("Dirección IP (ej. 172.20.251.253)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = prefijoTexto,
                        onValueChange = { prefijoTexto = it },
                        label = { Text("Prefijo CIDR (ej. 21)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (modoSugerencia.value) {
                    TextField(
                        value = valor, // Usar el mismo campo que para hosts
                        onValueChange = { valor = it },
                        label = { Text("Cantidad de dispositivos (hosts) requeridos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val n = valor.text.toIntOrNull()
                        if (n == null || n <= 0) {
                            sugerenciaResultado = null
                            sugerenciaExplicacion = "Por favor, ingresa un número válido de dispositivos mayor a 0."
                        } else {
                            val clase: String
                            val rango: String
                            val mascara: String
                            val explicacion: String
                            when {
                                n <= 254 -> {
                                    clase = "C"
                                    rango = "192.168.0.0 – 192.168.255.255"
                                    mascara = "255.255.255.0 (/24)"
                                    explicacion = "La clase C permite hasta 254 hosts válidos por red. Es ideal para redes pequeñas o domésticas."
                                }
                                n <= 65534 -> {
                                    clase = "B"
                                    rango = "172.16.0.0 – 172.31.255.255"
                                    mascara = "255.255.0.0 (/16)"
                                    explicacion = "La clase B permite hasta 65,534 hosts válidos por red. Es adecuada para redes medianas."
                                }
                                n <= 16777214 -> {
                                    clase = "A"
                                    rango = "10.0.0.0 – 10.255.255.255"
                                    mascara = "255.0.0.0 (/8)"
                                    explicacion = "La clase A permite hasta 16,777,214 hosts válidos por red. Es ideal para grandes organizaciones."
                                }
                                else -> {
                                    clase = "No disponible"
                                    rango = "-"
                                    mascara = "-"
                                    explicacion = "No existe una clase estándar que soporte esa cantidad de dispositivos en una sola red. Considera dividir en varias subredes o usar direccionamiento especial."
                                }
                            }
                            sugerenciaResultado = "Clase sugerida: $clase\nRango: $rango\nMáscara: $mascara"
                            sugerenciaExplicacion = "Explicación: $explicacion\n\nRecuerda que el número de hosts válidos por red es menor al total por la IP de red y broadcast.\n\nSi necesitas una subred exacta, considera usar subneteo para ajustar la máscara."
                        }
                    }) {
                        Text("Sugerir clase de red")
                    }
                    Spacer(Modifier.height(12.dp))
                    if (sugerenciaResultado != null) {
                        Text(sugerenciaResultado!!, style = MaterialTheme.typography.h6)
                    }
                    if (sugerenciaExplicacion.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(sugerenciaExplicacion, style = MaterialTheme.typography.body2)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { modoSugerencia.value = false; cantidadIPs = ""; sugerenciaResultado = null; sugerenciaExplicacion = "" }) {
                        Text("Cerrar sugerencia")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(onClick = {
                    if (modoPorIP) {
                        val ip = ipTexto.text
                        val prefijo = prefijoTexto.text.toIntOrNull()
                        if (prefijo != null) {
                            resultado = SubneteoCalculator.calcularPorIPyPrefijo(ip, prefijo)
                        } else {
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar("⚠️ Prefijo inválido")
                            }
                        }
                    } else {
                        val num = valor.text.toIntOrNull()
                        if (num != null) {
                            resultado = if (modoPorHosts)
                                SubneteoCalculator.calcularPorHosts(clase, num)
                            else
                                SubneteoCalculator.calcularPorPrefijo(clase, num)
                        } else {
                            resultado = null
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar("⚠️ Ingresa un número válido.")
                            }
                        }
                    }
                }) {
                    Text("Calcular")
                }

                Spacer(Modifier.height(20.dp))

                resultado?.let {
                    Column(Modifier.horizontalScroll(horizontalScroll)) {
                        Text("\nResultado:")
                        Text("Dirección de red: ${it.direccionRed} (${it.direccionRedBin})")
                        Text("Máscara de subred: ${it.mascaraSubred} (${it.mascaraBinaria})")
                        Text("Broadcast: ${it.direccionBroadcast} (${it.direccionBroadcastBin})")
                        Text("Primera IP válida: ${it.ipInicial}")
                        Text("Última IP válida: ${it.ipFinal}")
                        Text("Hosts válidos: ${it.cantidadHosts}")

                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { explicacionPopup = true }) {
                            Text("📘 Explicación detallada")
                        }
                    }
                }

                if (explicacionPopup) {
                    AlertDialog(
                        onDismissRequest = { explicacionPopup = false },
                        title = { Text("Explicación") },
                        text = { Text(resultado?.explicacion ?: "") },
                        confirmButton = {
                            Button(onClick = { explicacionPopup = false }) {
                                Text("Cerrar")
                            }
                        }
                    )
                }

             if (leyendaPopup) {
                    LeyendaPopup(onDismiss = { leyendaPopup = false })
                }
                if (calculadoraSubredesPopup) {
                    CalculadoraSubredesPopup(onDismiss = { calculadoraSubredesPopup = false })
                }
                if (conversorPopup) {
                    ConversorDecimalBinarioPopup(onDismiss = { conversorPopup = false })
                }
            }
        }
    }
}

@Composable
fun DropdownMenuSelector(current: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(current)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { label ->
                DropdownMenuItem(onClick = {
                    onSelected(label)
                    expanded = false
                }) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
fun TableRow(label: String, valor: String, binario: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Text(valor, modifier = Modifier.weight(1f))
            Text(binario, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CalculadoraSubredesPopup(onDismiss: () -> Unit) {
    var bitsRed by remember { mutableStateOf(24) } // Valor inicial típico para clase C
    var clase by remember { mutableStateOf("C") } // Clase seleccionada
    var resultado by remember { mutableStateOf<String?>(null) }
    var explicacion by remember { mutableStateOf("") }
    val clases = listOf("A", "B", "C")
    val bitsPorDefecto = mapOf("A" to 8, "B" to 16, "C" to 24)
    val ipBasePorClase = mapOf(
        "A" to "10.0.0.0",
        "B" to "172.16.0.0",
        "C" to "192.168.0.0"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculadora de Subredes") },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(min = 220.dp, max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 600.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecciona la clase de red:")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        clases.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = clase == c, onClick = { clase = c })
                                Text("Clase $c", modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Ingresa la cantidad de bits de red (prefijo CIDR):")
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("/")
                        TextField(
                            value = bitsRed.toString(),
                            onValueChange = {
                                val v = it.toIntOrNull()
                                if (v != null && v in 1..30) bitsRed = v
                            },
                            singleLine = true,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(" (ejemplo: 20)")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val bitsDefecto = bitsPorDefecto[clase] ?: 8
                        val bitsSubred = bitsRed - bitsDefecto
                        if (bitsSubred < 0) {
                            resultado = null
                            explicacion = "El prefijo debe ser mayor o igual al de la clase ($bitsDefecto)."
                        } else {
                            val numSubredes = 2.0.pow(bitsSubred).toInt()
                            val salto = 256 / numSubredes
                            val octetoSalto = when (clase) {
                                "A" -> 2 // segundo octeto
                                "B" -> 3 // tercer octeto
                                else -> 4 // cuarto octeto
                            }
                            val ejemplos = StringBuilder()
                            val ipBase = ipBasePorClase[clase] ?: "0.0.0.0"
                            val partes = ipBase.split(".").map { it.toInt() }.toMutableList()
                            for (i in 0 until minOf(numSubredes, 5)) {
                                when (octetoSalto) {
                                    2 -> partes[1] = i * salto
                                    3 -> partes[2] = i * salto
                                    4 -> partes[3] = i * salto
                                }
                                ejemplos.append("${partes[0]}.${partes[1]}.${partes[2]}.${partes[3]}\n")
                            }
                            resultado = "Número de subredes posibles: $numSubredes\nSalto entre subredes: $salto en el octeto ${octetoSalto}"
                            explicacion = """
                                Fórmula: 2^(bits de subred) = número de subredes
                                Donde bits de subred = prefijo - bits por defecto de la clase ($bitsDefecto)
                                Salto = 256 / número de subredes en el octeto correspondiente
                                
                                Ejemplo para clase $clase, prefijo /$bitsRed:
                                IP base: $ipBase
                                """.trimIndent() +
                                "\nPrimeras subredes generadas:" +
                                "\n" +
                                ejemplos.toString() +
                                "\nSi tienes una clase B (172.16.0.0) y prefijo /20:" +
                                "\n- Bits por defecto: 16" +
                                "\n- Bits de subred: 20-16=4" +
                                "\n- Subredes: 2^4 = 16" +
                                "\n- Salto: 256/16 = 16 en el tercer octeto" +
                                "\n- Subredes: 172.16.0.0, 172.16.16.0, 172.16.32.0, ..."
                        }
                    }) {
                        Text("Calcular subredes")
                    }
                    Spacer(Modifier.height(12.dp))
                    if (resultado != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(resultado!!, style = MaterialTheme.typography.h6)
                        }
                    }
                    if (explicacion.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(explicacion, style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun ConversorDecimalBinarioPopup(onDismiss: () -> Unit) {
    var decimalInput by remember { mutableStateOf("") }
    var binaryOutput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun convertToBinary(decimal: Int) {
        binaryOutput = decimal.toString(2)
        errorMessage = ""
    }

    fun clearFields() {
        decimalInput = ""
        binaryOutput = ""
        errorMessage = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conversor Decimal a Binario") },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(min = 150.dp, max = 350.dp)
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 600.dp) // Limita el ancho máximo para evitar constraints infinitos
            ) {
                Column {
                    TextField(
                        value = decimalInput,
                        onValueChange = {
                            decimalInput = it
                            // Convierte a binario al cambiar el valor, si es un número válido
                            val decimal = it.toIntOrNull()
                            if (decimal != null) {
                                convertToBinary(decimal)
                            } else {
                                binaryOutput = ""
                            }
                        },
                        label = { Text("Número decimal") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.isNotEmpty()
                    )
                    // Muestra un mensaje de error si existe
                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = MaterialTheme.colors.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Resultado en binario:")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .widthIn(max = 600.dp) // Limita el ancho máximo aquí también
                    ) {
                        Text(
                            binaryOutput,
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    // Botones para copiar o limpiar
                    Row {
                        Button(onClick = {
                            // Copia el resultado en binario al portapapeles
                            // (implementación del portapapeles no incluida en este fragmento)
                        }) {
                            Text("📋 Copiar resultado")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            clearFields() // Limpia los campos
                        }) {
                            Text("🧹 Limpiar", fontSize = 12.sp) // Solo el texto es más pequeño
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun LeyendaPopup(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leyenda de Clases IP y Rangos Privados") },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(min = 100.dp, max = 300.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
               Text(
                        """
                        Clase A: 1.0.0.0 – 126.255.255.255 (255.0.0.0 /8) → Grandes redes
                        Clase B: 128.0.0.0 – 191.255.255.255 (255.255.0.0 /16) → Redes medianas
                        Clase C: 192.0.0.0 – 223.255.255.255 (255.255.255.0 /24) → Redes pequeñas
                        Clase D: 224.0.0.0 – 239.255.255.255 → Multicast, no se usa en subneteo
                        Clase E: 240.0.0.0 – 255.255.255.255 → Reservada para investigación
                        Clase E: 240.0.0.0 – 255.255.255.255 → Reservada para investigación
                    
                        Rangos de IP privadas:
                        - Clase A: 10.0.0.0 – 10.255.255.255 (10.0.0.0/8)
                        - Clase B: 172.16.0.0 – 172.31.255.255 (172.16.0.0/12)
                        - Clase C: 192.168.0.0 – 192.168.255.255 (192.168.0.0/16)
                    
                        Estas direcciones privadas no son enrutable en Internet y se usan en redes internas.
                        """.trimIndent()
                    )
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cerrar")
            }
        }
    )
}
