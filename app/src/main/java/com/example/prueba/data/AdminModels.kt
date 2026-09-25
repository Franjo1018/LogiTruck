package com.example.prueba.data

// Modelos compartidos por las pantallas de administrador/despachador.
// Corresponden a las tablas USUARIO, VEHICULO, VIAJE, NOTIFICACION y MENSAJE del ERD.
// TODO: mover a /data/model cuando conectes Room; aquí solo describen la forma de la UI.

enum class EstadoConductor(val etiqueta: String) {
    DISPONIBLE("Disponible"),
    EN_RUTA("En ruta"),
    DESCANSO("Descanso")
}

data class Conductor(
    val id: Long,
    val nombre: String,
    val estado: EstadoConductor,
    val licenciaVigente: Boolean,
    val vehiculoAsignado: String? = null,
    // Key real del usuario en Firebase (empresas/{empresaId}/usuarios/{uid}). Vacío en los
    // conductores de ejemplo que todavía usan otras pantallas (CrearViajeScreen); lo llena
    // ConductorRepository al leer conductores reales para GestionConductoresScreen.
    val uid: String = "",
    // Usuario (login) real de la persona, p.ej. "carlosmendoza". Vacío en datos de ejemplo.
    val usuario: String = ""
)

enum class EstadoVehiculo(val etiqueta: String) {
    OPERATIVO("Operativo"),
    EN_MANTENIMIENTO("En mantenimiento"),
    FUERA_DE_SERVICIO("Fuera de servicio")
}

data class DocumentoVehiculo(
    val nombre: String, // "SOAT", "Revisión técnica"
    val vigente: Boolean,
    val vencimiento: String
)

data class Vehiculo(
    val id: Long,
    val placa: String,
    val modelo: String,
    val estado: EstadoVehiculo,
    val documentos: List<DocumentoVehiculo>,
    val proximoMantenimientoKm: Int,
    // Key real del vehículo en Firebase (empresas/{empresaId}/vehiculos/{idReal}). Vacío en
    // los vehículos de ejemplo que todavía usa CrearViajeScreen; lo llena VehiculoRepository
    // al leer los vehículos reales para GestionVehiculosScreen.
    val idReal: String = "",
    val conductorAsignadoUid: String? = null,
    val conductorAsignadoNombre: String? = null
)

enum class SeveridadIncidencia(val etiqueta: String) {
    LEVE("Leve"),
    MODERADA("Moderada"),
    GRAVE("Grave")
}

data class Incidencia(
    val id: Long,
    val viajeOrigenDestino: String,
    val conductor: String,
    val descripcion: String,
    val severidad: SeveridadIncidencia,
    val fecha: String,
    val resuelta: Boolean,
    // Key real en Firebase (empresas/{empresaId}/incidencias/{idReal}) y uid del conductor que
    // la reportó. Vacíos hasta que IncidenciaRepository los llena con datos reales.
    val idReal: String = "",
    val conductorUid: String = "",
    // URL de descarga en Firebase Storage de la foto de evidencia adjuntada al reportar (ver
    // AlmacenamientoRepository). Vacío si no se adjuntó foto.
    val fotoUrl: String = ""
)

data class MensajeChat(
    val id: Long,
    val autor: String,
    val texto: String,
    val hora: String,
    val esPropio: Boolean,
    val idReal: String = "",
    val autorUid: String = ""
)

data class ConversacionResumen(
    val id: Long,
    val nombreConductor: String,
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int,
    // uid del conductor: identifica el hilo (empresas/{empresaId}/mensajes/{conductorUid}).
    val conductorUid: String = ""
)

/** Última ubicación GPS conocida de un conductor (empresas/{empresaId}/ubicaciones/{conductorUid}). */
data class UbicacionConductor(
    val conductorUid: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val timestampMs: Long
)

data class AlertaDashboard(
    val texto: String,
    val severidad: SeveridadIncidencia
)

data class KpisFlota(
    val viajesActivos: Int,
    val viajesCompletadosMes: Int,
    val incidenciasAbiertas: Int,
    val huellaCarbonoKg: Double
)

enum class EstadoViaje(val etiqueta: String) {
    PROGRAMADO("Programado"),
    EN_RUTA("En ruta"),
    COMPLETADO("Completado")
}

/**
 * Un viaje asignado por el admin/despachador a un conductor y un vehículo (tabla VIAJE),
 * en Realtime Database bajo empresas/{empresaId}/viajes/{idReal}. Mientras el conductor tenga
 * un viaje en estado distinto de COMPLETADO, ese es su "viaje activo": ConductorHostScreen usa
 * esto para habilitar recién ahí el checklist previaje (antes de tener un viaje asignado no
 * tiene sentido mostrárselo).
 */
data class Viaje(
    val idReal: String,
    val conductorUid: String,
    val conductorNombre: String,
    val vehiculoIdReal: String,
    val vehiculoPlaca: String,
    val origen: String,
    val destino: String,
    val fecha: String,
    val hora: String,
    val descripcionCarga: String,
    val pesoCargaKg: String,
    val estado: EstadoViaje,
    val timestampMs: Long
)

/**
 * Registro de combustible cargado. Se mide en GALONES (no litros): es la unidad con la que
 * normalmente se factura y se registra el combustible en Perú.
 *
 * No es un registro único por viaje: un mismo viaje puede tener varias cargas (la inicial que
 * se declara en el checklist previaje, y luego una o más recargas en ruta desde
 * RegistroCombustibleScreen). viajeIdReal identifica a cuál viaje pertenece cada carga, para
 * poder verlas agrupadas por viaje (ViajeCargaDetalleScreen) y sumarlas correctamente en el
 * reporte de huella de carbono. Vacío en cargas antiguas de conductores sin viaje asociado.
 */
data class RegistroCombustible(
    val galones: Double,
    val costo: Double,
    val lugar: String,
    val hora: String,
    val idReal: String = "",
    val fecha: String = "",
    val conductorUid: String = "",
    val conductorNombre: String = "",
    val viajeIdReal: String = ""
)

/**
 * Checklist previaje completado por el conductor antes de iniciar un viaje (empresas/{empresaId}/
 * viajes/{idReal}/checklist). Incluye el estado de cada ítem de inspección más los datos que se
 * declaran junto con el checklist: kilometraje con el que sale el vehículo y si se adjuntó foto
 * de la factura de la carga inicial de combustible (esa carga en sí se guarda como un
 * RegistroCombustible normal, ligado al mismo viajeIdReal).
 */
data class ChecklistPrevio(
    val items: Map<String, Boolean>,
    val kilometrajeInicial: String,
    val facturaCombustibleAdjunta: Boolean,
    val completadoEn: Long,
    // URL de descarga en Firebase Storage de la foto de la factura (ver AlmacenamientoRepository).
    // Vacío si no se adjuntó foto (facturaCombustibleAdjunta puede ser true por una versión
    // vieja de la app que solo guardaba el flag, sin la foto real).
    val facturaCombustibleUrl: String = ""
)
