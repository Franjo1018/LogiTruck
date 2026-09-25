package com.example.prueba.screens.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.prueba.components.DrawerHeader
import com.example.prueba.components.coloresMenuLateral
import com.example.prueba.data.ConversacionResumen
import com.example.prueba.data.local.Rol
import com.example.prueba.data.local.UsuarioEntity
import kotlinx.coroutines.launch

/** Secciones del panel de administrador/despachador accesibles desde el menú lateral. */
private enum class SeccionAdmin(val titulo: String, val icono: ImageVector) {
    DASHBOARD("Panel", Icons.Filled.Dashboard),
    FLOTA("Flota en tiempo real", Icons.Filled.Map),
    CONDUCTORES("Conductores", Icons.Filled.Groups),
    VEHICULOS("Vehículos", Icons.Filled.LocalShipping),
    CREAR_VIAJE("Crear viaje", Icons.Filled.Route),
    INCIDENCIAS("Incidencias", Icons.Filled.ReportProblem),
    MENSAJERIA("Mensajería", Icons.Filled.Forum),
    HUELLA_CARBONO("Huella de carbono", Icons.Filled.Eco),
    INVITAR("Invitar a la empresa", Icons.Filled.PersonAdd),
    // No aparece en el menú lateral (se filtra abajo): solo se llega aquí desde el botón "+"
    // de la pantalla de Vehículos.
    VEHICULO_NUEVO("Registrar vehículo", Icons.Filled.LocalShipping)
}

/**
 * Contenedor del panel de administrador/despachador: agrega un menú lateral (drawer) para
 * moverse entre las pantallas de admin, más "Invitar a la empresa" (genera códigos para que
 * otras personas se unan a esta misma empresa) y "Cerrar sesión". No se envuelve todo en un
 * Scaffold propio porque cada pantalla ya trae su propia TopAppBar; en su lugar, se superpone
 * un ícono de menú (☰) en la esquina superior izquierda, en el mismo lugar donde normalmente
 * va el ícono de navegación de un TopAppBar (esa franja queda vacía porque las pantallas no le
 * pasan un navigationIcon propio), así se ve como parte natural de la barra superior en vez de
 * un botón flotante aparte escondido abajo.
 *
 * El encabezado (DrawerHeader) y los colores del menú (naranja/plomo, coloresMenuLateral) usan
 * la misma paleta que LoginScreen en vez del lila por defecto de Material, para que se sienta
 * parte de la misma app.
 * TODO: si el proyecto migra a Navigation Compose, esto se reemplaza por un NavHost con rutas.
 */
@Composable
fun AdminHostScreen(usuario: UsuarioEntity, onCerrarSesion: () -> Unit) {
    var seccionActual by remember { mutableStateOf(SeccionAdmin.DASHBOARD) }
    var conversacionSeleccionada by remember { mutableStateOf<ConversacionResumen?>(null) }
    var incidenciaSeleccionada by remember { mutableStateOf<com.example.prueba.data.Incidencia?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val rolEtiqueta = when (usuario.rol) {
        Rol.ADMINISTRADOR -> "Administrador"
        Rol.DESPACHADOR -> "Despachador"
        Rol.CONDUCTOR -> "Conductor"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // El gesto de deslizar desde el borde para abrir el menú choca con arrastrar el mapa en
        // "Flota en tiempo real" (MapLibre necesita ese mismo gesto para mover la cámara), así
        // que ahí se desactiva; el menú se sigue abriendo con el ícono ☰ en esa pantalla. En el
        // resto de secciones el deslizar para abrir el menú sigue funcionando normal.
        gesturesEnabled = seccionActual != SeccionAdmin.FLOTA,
        drawerContent = {
            // Ancho fijo (65% de la pantalla) en vez del ancho por defecto de ModalDrawerSheet,
            // que en pantallas angostas casi tapa toda la vista actual; así se sigue viendo
            // parte de la pantalla de fondo mientras el menú está abierto.
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.65f)) {
                DrawerHeader(nombre = usuario.nombreCompleto, subtitulo = rolEtiqueta)
                LazyColumn {
                    items(SeccionAdmin.entries.filter { it != SeccionAdmin.VEHICULO_NUEVO }) { seccion ->
                        NavigationDrawerItem(
                            label = { Text(seccion.titulo) },
                            selected = seccion == seccionActual,
                            icon = { Icon(seccion.icono, contentDescription = null) },
                            colors = coloresMenuLateral(),
                            onClick = {
                                seccionActual = seccion
                                if (seccion == SeccionAdmin.MENSAJERIA) conversacionSeleccionada = null
                                if (seccion == SeccionAdmin.INCIDENCIAS) incidenciaSeleccionada = null
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Cerrar sesión") },
                            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
                            selected = false,
                            colors = coloresMenuLateral(),
                            onClick = {
                                scope.launch { drawerState.close() }
                                onCerrarSesion()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (seccionActual) {
                SeccionAdmin.DASHBOARD -> AdminDashboardScreen(
                    empresaId = usuario.empresaId,
                    nombre = usuario.nombreCompleto,
                    onAlertaClick = {
                        incidenciaSeleccionada = null
                        seccionActual = SeccionAdmin.INCIDENCIAS
                    },
                    onVerFlotaClick = { seccionActual = SeccionAdmin.FLOTA }
                )
                SeccionAdmin.FLOTA -> FlotaMapaScreen(empresaId = usuario.empresaId)
                SeccionAdmin.CONDUCTORES -> GestionConductoresScreen(
                    empresaId = usuario.empresaId,
                    onAgregarClick = { seccionActual = SeccionAdmin.INVITAR }
                )
                SeccionAdmin.VEHICULOS -> GestionVehiculosScreen(
                    empresaId = usuario.empresaId,
                    onAgregarClick = { seccionActual = SeccionAdmin.VEHICULO_NUEVO }
                )
                SeccionAdmin.CREAR_VIAJE -> CrearViajeScreen(empresaId = usuario.empresaId)
                SeccionAdmin.INCIDENCIAS -> {
                    val incidencia = incidenciaSeleccionada
                    if (incidencia == null) {
                        HistorialIncidenciasScreen(
                            empresaId = usuario.empresaId,
                            onIncidenciaClick = { incidenciaSeleccionada = it }
                        )
                    } else {
                        ConversacionChatScreen(
                            empresaId = usuario.empresaId,
                            conductorUid = incidencia.conductorUid,
                            viewerUid = usuario.uid,
                            viewerNombre = usuario.nombreCompleto,
                            esDeAdmin = true,
                            nombreConductor = incidencia.conductor,
                            incidencia = incidencia,
                            onVolverClick = { incidenciaSeleccionada = null }
                        )
                    }
                }
                SeccionAdmin.MENSAJERIA -> {
                    val conversacion = conversacionSeleccionada
                    if (conversacion == null) {
                        MensajeriaScreen(
                            empresaId = usuario.empresaId,
                            onConversacionClick = { conversacionSeleccionada = it }
                        )
                    } else {
                        ConversacionChatScreen(
                            empresaId = usuario.empresaId,
                            conductorUid = conversacion.conductorUid,
                            viewerUid = usuario.uid,
                            viewerNombre = usuario.nombreCompleto,
                            esDeAdmin = true,
                            nombreConductor = conversacion.nombreConductor,
                            onVolverClick = { conversacionSeleccionada = null }
                        )
                    }
                }
                SeccionAdmin.HUELLA_CARBONO -> ReporteHuellaCarbonoScreen(empresaId = usuario.empresaId)
                SeccionAdmin.INVITAR -> GenerarInvitacionScreen(empresaId = usuario.empresaId)
                SeccionAdmin.VEHICULO_NUEVO -> RegistrarVehiculoScreen(
                    empresaId = usuario.empresaId,
                    onVehiculoRegistrado = { seccionActual = SeccionAdmin.VEHICULOS }
                )
            }

            // Ícono de menú superpuesto en la esquina superior izquierda, en el mismo lugar
            // donde va el ícono de navegación de un TopAppBar normal (esa franja de 56dp queda
            // vacía en las pantallas de abajo porque no le pasan un navigationIcon propio), así
            // se ve como una barrita de menú integrada en vez de un botón flotante aparte.
            // statusBarsPadding() lo baja para que no quede tapado por la barra de notificaciones/
            // señal/batería del celular.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .size(width = 56.dp, height = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Abrir menú de secciones",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
