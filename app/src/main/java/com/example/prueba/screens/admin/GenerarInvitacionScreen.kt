@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import com.example.prueba.data.local.Rol
import com.example.prueba.data.remote.RealtimeDb
import kotlinx.coroutines.launch

/**
 * Pantalla para que un administrador genere un código de invitación: cualquier persona con
 * ese código puede registrarse (pantalla "Unirme con código") y queda automáticamente en la
 * misma empresa, con el rol elegido aquí. El código se guarda en `invitaciones/{codigo}`;
 * las reglas de Firebase solo dejan escribir ahí a administradores de la empresa que va en
 * el código (ver database.rules.json).
 */
@Composable
fun GenerarInvitacionScreen(empresaId: String) {
    var rolSeleccionado by remember { mutableStateOf(Rol.CONDUCTOR) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var generando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Generar invitación") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            Text("Rol para la persona que se una con este código:", fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                listOf(Rol.CONDUCTOR, Rol.DESPACHADOR).forEach { rol ->
                    val seleccionado = rol == rolSeleccionado
                    val etiqueta = if (rol == Rol.CONDUCTOR) "Conductor" else "Despachador"
                    if (seleccionado) {
                        Button(
                            onClick = { rolSeleccionado = rol },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
                        ) { Text(etiqueta) }
                    } else {
                        OutlinedButton(
                            onClick = { rolSeleccionado = rol },
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) { Text(etiqueta) }
                    }
                }
            }

            Button(
                onClick = {
                    error = null
                    generando = true
                    scope.launch {
                        try {
                            val codigo = (100000..999999).random().toString()
                            RealtimeDb.escribir(
                                "invitaciones/$codigo",
                                mapOf("empresaId" to empresaId, "rol" to rolSeleccionado.name)
                            )
                            codigoGenerado = codigo
                        } catch (e: Exception) {
                            error = "No se pudo generar el código, revisa tu conexión"
                        } finally {
                            generando = false
                        }
                    }
                },
                enabled = !generando,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
            ) {
                Text(if (generando) "Generando..." else "Generar código")
            }

            codigoGenerado?.let { codigo ->
                Text(
                    text = codigo,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaLogicTruck,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    text = "Comparte este código con la persona que quieres invitar. Lo va a usar en " +
                        "\"Crear cuenta\" → \"Unirme con código\".",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            error?.let {
                Text(text = it, color = Color(0xFFB3261E), modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
