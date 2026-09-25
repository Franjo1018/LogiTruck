package com.example.prueba.screens.conductor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReportProblem
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.DrawerHeader
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresMenuLateral
import com.example.prueba.data.EstadoViaje
import com.example.prueba.data.UbicacionRepository
import com.example.prueba.data.Viaje
import com.example.prueba.data.ViajeRepository
import com.example.prueba.data.local.UsuarioEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Secciones del panel de conductor accesibles desde el menú lateral. */
private enum class SeccionConductor(val titulo: String, val icono: ImageVector) {
    CHECKLIST("Checklist previaje", Icons.Filled.CheckCircle),
    HISTORIAL("Historial de viajes", Icons.Filled.History),
    COMBUSTIBLE("Registro de combustible", Icons.Filled.LocalGasStation),
    INCIDENCIA("Reportar incidencia", Icons.Filled.ReportProblem),
    MENSAJES("Mensajería", Icons.Filled.Forum)
}

/** Cada cuántos metros de cambio de posición se reporta la ubicación nueva a Firebase. */
private const val DISTANCIA_MIN_METROS = 30f

/** Cada cuánto tiempo (ms) se reporta la ubicación como mínimo, aunque no haya señal de GPS nueva. */
private const val INTERVALO_MIN_MS = 20_000L

/** Cada cuánto se revisa si al conductor ya le asignaron un viaje (mientras no tiene uno activo). */
private const val INTERVALO_REVISION_VIAJE_MS = 15_000L

/**
 * Contenedor del panel de conductor: agrega un menú lateral (drawer) para moverse entre
 * ChecklistPreviajeScreen, HistorialViajesScreen, RegistroCombustibleScreen y
 * ReporteIncidenciaScreen, más "Cerrar sesión" — mismo patrón que AdminHostScreen.
 *
 * El checklist previaje solo se habilita cuando el conductor tiene un viaje activo (asignado
 * por el despachador y todavía no completado, ver ViajeRepository.viajeActivoPorConductor): si
 * no tiene ninguno, en su lugar se muestra SinViajeAsignadoScreen. Se revisa cada 15s mientras
 * no haya viaje activo, y en cuanto aparece uno se muestra un banner naranja avisando que ya
 * tiene un viaje habilitado.
 *
 * También reporta la ubicación GPS del conductor a Firebase (empresas/{empresaId}/ubicaciones/
 * {uid} vía UbicacionRepository) para que el admin la vea en tiempo real en FlotaMapaScreen.
 * Por privacidad, esto SOLO ocurre mientras el conductor tiene un viaje activo: en cuanto deja
 * de tenerlo (viaje completado, o todavía no le asignaron ninguno) se dejan de pedir
 * actualizaciones de GPS y se borra su última ubicación conocida, así no queda visible en el
 * mapa fuera de un viaje. Pide el permiso de ubicación la primera vez que se abre.
 */
@SuppressLint("MissingPermission")
@Composable
fun ConductorHostScreen(usuario: UsuarioEntity, onCerrarSesion: () -> Unit) {
    var seccionActual by remember { mutableStateOf(SeccionConductor.CHECKLIST) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var viajeActivo by remember { mutableStateOf<Viaje?>(null) }
    var mostrarBannerViaje by remember { mutableStateOf(false) }
    var ultimoIdRealVistoEnBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(usuario.empresaId, usuario.uid) {
        while (true) {
            try {
                val nuevoViajeActivo = ViajeRepository.viajeActivoPorConductor(usuario.empresaId, usuario.uid)
                if (nuevoViajeActivo != null && nuevoViajeActivo.idReal != ultimoIdRealVistoEnBanner) {
                    mostrarBannerViaje = true
                    ultimoIdRealVistoEnBanner = nuevoViajeActivo.idReal
                }
                // Sin viaje activo: por privacidad se borra la última ubicación reportada, para
                // que el conductor no quede visible en el mapa del admin fuera de un viaje.
                if (nuevoViajeActivo == null && viajeActivo != null) {
                    runCatching { UbicacionRepository.eliminar(usuario.empresaId, usuario.uid) }
                }
                viajeActivo = nuevoViajeActivo
            } catch (e: Exception) {
                // Sin conexión en este ciclo: se mantiene el último estado conocido.
            }
            delay(INTERVALO_REVISION_VIAJE_MS)
        }
    }

    var permisoUbicacionConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val lanzadorPermisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> permisoUbicacionConcedido = concedido }

    LaunchedEffect(Unit) {
        if (!permisoUbicacionConcedido) {
            lanzadorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(permisoUbicacionConcedido, usuario.uid, viajeActivo?.idReal) {
        if (!permisoUbicacionConcedido || viajeActivo == null) return@DisposableEffect onDispose {}
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = android.location.LocationListener { ubicacion ->
            scope.launch {
                runCatching {
                    UbicacionRepository.actualizar(
                        empresaId = usuario.empresaId,
                        conductorUid = usuario.uid,
                        nombre = usuario.nombreCompleto,
                        lat = ubicacion.latitude,
                        lon = ubicacion.longitude
                    )
                }
            }
        }
        val proveedor = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (proveedor != null) {
            locationManager.requestLocationUpdates(proveedor, INTERVALO_MIN_MS, DISTANCIA_MIN_METROS, listener)
        }
        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Mismo ancho fijo (65%) que AdminHostScreen, para que el menú no tape toda la
            // pantalla y se siga viendo parte de la vista de fondo mientras está abierto.
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.65f)) {
                DrawerHeader(nombre = usuario.nombreCompleto, subtitulo = "Conductor")
                LazyColumn {
                    items(SeccionConductor.entries) { seccion ->
                        NavigationDrawerItem(
                            label = { Text(seccion.titulo) },
                            selected = seccion == seccionActual,
                            icon = { Icon(seccion.icono, contentDescription = null) },
                            colors = coloresMenuLateral(),
                            onClick = {
                                seccionActual = seccion
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
                SeccionConductor.CHECKLIST -> {
                    val viaje = viajeActivo
                    when {
                        viaje == null -> SinViajeAsignadoScreen(nombre = usuario.nombreCompleto)
                        // El checklist previaje de este viaje ya se completó una vez (el viaje ya
                        // pasó de PROGRAMADO a EN_RUTA): no tiene sentido mostrar de nuevo el
                        // formulario con checkboxes vacíos, así que se muestra la vista de solo
                        // lectura con la confirmación y el resumen de lo ya declarado.
                        viaje.estado != EstadoViaje.PROGRAMADO -> ChecklistCompletadoScreen(
                            empresaId = usuario.empresaId,
                            viajeIdReal = viaje.idReal,
                            nombre = usuario.nombreCompleto,
                            origenDestino = "${viaje.origen} → ${viaje.destino}"
                        )
                        else -> ChecklistPreviajeScreen(
                            empresaId = usuario.empresaId,
                            viajeIdReal = viaje.idReal,
                            conductorUid = usuario.uid,
                            conductorNombre = usuario.nombreCompleto,
                            nombre = usuario.nombreCompleto,
                            origenDestino = "${viaje.origen} → ${viaje.destino}",
                            onIniciarViajeClick = {
                                scope.launch {
                                    runCatching {
                                        ViajeRepository.marcarEstado(usuario.empresaId, viaje.idReal, EstadoViaje.EN_RUTA)
                                    }
                                }
                            }
                        )
                    }
                }
                SeccionConductor.HISTORIAL -> HistorialViajesScreen(
                    empresaId = usuario.empresaId,
                    conductorUid = usuario.uid
                )
                SeccionConductor.COMBUSTIBLE -> RegistroCombustibleScreen(
                    empresaId = usuario.empresaId,
                    conductorUid = usuario.uid,
                    conductorNombre = usuario.nombreCompleto,
                    viajeIdReal = viajeActivo?.idReal ?: ""
                )
                SeccionConductor.INCIDENCIA -> ReporteIncidenciaScreen(
                    empresaId = usuario.empresaId,
                    conductorUid = usuario.uid,
                    conductorNombre = usuario.nombreCompleto,
                    origenDestino = viajeActivo?.let { "${it.origen} → ${it.destino}" } ?: "Sin ruta asignada"
                )
                SeccionConductor.MENSAJES -> MensajeriaConductorScreen(
                    empresaId = usuario.empresaId,
                    conductorUid = usuario.uid,
                    conductorNombre = usuario.nombreCompleto
                )
            }

            // Ícono de menú superpuesto en la esquina superior izquierda, en el mismo lugar
            // donde va el ícono de navegación de un TopAppBar normal. statusBarsPadding() lo
            // baja para que no quede tapado por la barra de notificaciones/señal/batería.
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

            // Banner que avisa que ya se habilitó un viaje nuevo, sin importar en qué sección
            // esté el conductor; se puede cerrar o tocar para ir directo al checklist.
            val viajeParaBanner = viajeActivo
            if (mostrarBannerViaje && viajeParaBanner != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NaranjaLogicTruck)
                        .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
                        .clickable {
                            mostrarBannerViaje = false
                            seccionActual = SeccionConductor.CHECKLIST
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Viaje habilitado", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                            "${viajeParaBanner.origen} → ${viajeParaBanner.destino}. Toca aquí para ir al checklist.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { mostrarBannerViaje = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar aviso", tint = Color.White)
                    }
                }
            }
        }
    }
}
