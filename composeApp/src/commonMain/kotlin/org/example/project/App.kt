// App.kt COMPLETO Y CORREGIDO CON OPCIÓN DE IP + PREFIJO
package org.example.project

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.subneteo.SubneteoCalculator
import org.example.project.subneteo.SubneteoResultado

@Composable
fun App() {
    MaterialTheme {
        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()

        var modoPorHosts by remember { mutableStateOf(true) }
        var modoPorIP by remember { mutableStateOf(false) }
        var clase by remember { mutableStateOf("A") }
        var valor by remember { mutableStateOf(TextFieldValue("")) }
        var ipTexto by remember { mutableStateOf(TextFieldValue("")) }
        var prefijoTexto by remember { mutableStateOf(TextFieldValue("")) }
        var resultado by remember { mutableStateOf<SubneteoResultado?>(null) }
        var explicacionPopup by remember { mutableStateOf(false) }
        var leyendaPopup by remember { mutableStateOf(false) }

        val verticalScroll = rememberScrollState()
        val horizontalScroll = rememberScrollState()

        Scaffold(scaffoldState = scaffoldState) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("🔧 Calculadora de Subneteo", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))

                Column {
                    Text("Clase de IP (elige el rango base para calcular el subneteo):")
                    Spacer(Modifier.height(4.dp))
                    DropdownMenuSelector(current = clase, options = listOf("A", "B", "C", "D", "E")) {
                        if (it in listOf("D", "E")) {
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    when (it) {
                                        "D" -> "❌ Clase D no disponible: es para multicast, no para direccionamiento de dispositivos."
                                        "E" -> "❌ Clase E reservada: se usa para investigación y pruebas."
                                        else -> "❌ Clase $it no permitida para subneteo."
                                    }
                                )
                            }
                        } else {
                            clase = it
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { leyendaPopup = true }) {
                        Text("ℹ️ Ver leyenda")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modoPorHosts, onClick = {
                            modoPorHosts = true; modoPorIP = false
                        })
                        Text("Por número de hosts")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !modoPorHosts && !modoPorIP, onClick = {
                            modoPorHosts = false; modoPorIP = false
                        })
                        Text("Por prefijo CIDR")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modoPorIP, onClick = {
                            modoPorIP = true; modoPorHosts = false
                        })
                        Text("Por dirección IP + prefijo")
                    }
                }

                Spacer(Modifier.height(8.dp))

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
                        title = { Text("Leyenda de Clases IP") },
                        text = {
                            Text(
                                """
                                Clase A: 1.0.0.0 – 126.255.255.255 (255.0.0.0) → Grandes redes
                                Clase B: 128.0.0.0 – 191.255.255.255 (255.255.0.0) → Redes medianas
                                Clase C: 192.0.0.0 – 223.255.255.255 (255.255.255.0) → Redes pequeñas
                                Clase D: 224.0.0.0 – 239.255.255.255 → Multicast, no se usa en subneteo
                                Clase E: 240.0.0.0 – 255.255.255.255 → Reservada para investigación
                                """.trimIndent()
                            )
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
