@file:OptIn(ExperimentalMaterial3Api::class)

package com.logictruck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val conversacionesDemo = listOf(
    ConversacionResumen(1, "Carlos Mendoza", "Ya llegué al grifo, cargando combustible", "6:52 pm", 1),
    ConversacionResumen(2, "Rosa Salinas", "Confirmado, salgo mañana 8am", "5:10 pm", 0),
    ConversacionResumen(3, "Jorge Huamán", "¿Puedo tomar la ruta alterna por el bloqueo?", "4:02 pm", 2)
)

private val mensajesDemoPorConversacion = listOf(
    MensajeChat(1, "Carlos Mendoza", "Voy llegando a Pativilca", "6:40 pm", esPropio = false),
    MensajeChat(2, "Tú", "Perfecto, avísame cualquier novedad", "6:41 pm", esPropio = true),
    MensajeChat(3, "Carlos Mendoza", "Ya llegué al grifo, cargando combustible", "6:52 pm", esPropio = false)
)

/**
 * Bandeja de conversaciones (tabla MENSAJE) entre administrador/despachador y conductores.
 * También sirve de punto de entrada para NOTIFICACION (avisos del sistema, no respondibles).
 * TODO: alimentar desde MensajeRepository + listener en tiempo real (Firebase) para push.
 */
@Composable
fun MensajeriaScreen(
    conversaciones: List<ConversacionResumen> = conversacionesDemo,
    onConversacionClick: (ConversacionResumen) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mensajería") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversaciones) { conversacion ->
                OutlinedCard(onClick = { onConversacionClick(conversacion) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iniciales = conversacion.nombreConductor.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .joinToString("")
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(iniciales, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(conversacion.nombreConductor, fontWeight = FontWeight.Medium)
                            Text(
                                conversacion.ultimoMensaje,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(conversacion.hora, style = MaterialTheme.typography.labelSmall)
                            if (conversacion.noLeidos > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        conversacion.noLeidos.toString(),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Vista de un chat individual con un conductor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversacionChatScreen(
    nombreConductor: String = "Carlos Mendoza",
    mensajes: List<MensajeChat> = mensajesDemoPorConversacion,
    onEnviarClick: (String) -> Unit = {},
    onVolverClick: () -> Unit = {}
) {
    var textoNuevo by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreConductor) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoNuevo,
                    onValueChange = { textoNuevo = it },
                    placeholder = { Text("Escribe un mensaje") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (textoNuevo.isNotBlank()) {
                        onEnviarClick(textoNuevo)
                        textoNuevo = ""
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mensajes) { mensaje ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mensaje.esPropio) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (mensaje.esPropio)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(mensaje.texto, style = MaterialTheme.typography.bodyMedium)
                            Text(mensaje.hora, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
