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
                }

                Spacer(Modifier.height(8.dp)) // Espacio vertical entre secciones

                Column {
                    // Opción para calcular por número de hosts
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modoPorHosts, onClick = {
                            modoPorHosts = true; modoPorIP = false // Activa modo hosts, desactiva modo IP
                        })
                        Text("Por número de hosts") // Etiqueta de la opción
                    }
                    // Opción para calcular por prefijo CIDR
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !modoPorHosts && !modoPorIP, onClick = {
                            modoPorHosts = false; modoPorIP = false // Activa modo prefijo, desactiva otros
                        })
                        Text("Por prefijo CIDR") // Etiqueta de la opción
                    }
                    // Opción para calcular por dirección IP y prefijo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modoPorIP, onClick = {
                            modoPorIP = true; modoPorHosts = false // Activa modo IP, desactiva modo hosts
                        })
                        Text("Por dirección IP + prefijo") // Etiqueta de la opción
                    }
                }

                Spacer(Modifier.height(8.dp)) // Espacio vertical entre secciones

                if (!modoPorIP) {
                    TextField(
                        value = valor,
                        onValueChange = { valor = it },
                        label = {
                            Text(if (modoPorHosts) "Número de hosts" else "Prefijo CIDR (ej. 18)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
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
                    AlertDialog(
                        onDismissRequest = { leyendaPopup = false },
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
                                    Clase A: 1.0.0.0 – 126.255.255.255 (255.0.0.0) → Grandes redes
                                    Clase B: 128.0.0.0 – 191.255.255.255 (255.255.0.0) → Redes medianas
                                    Clase C: 192.0.0.0 – 223.255.255.255 (255.255.255.0) → Redes pequeñas
                                    Clase D: 224.0.0.0 – 239.255.255.255 → Multicast, no se usa en subneteo
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
                            Button(onClick = { leyendaPopup = false }) {
                                Text("Cerrar")
                            }
                        }
                    )
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
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
