package com.logictruck.ui.screens

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
    val vehiculoAsignado: String? = null
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
    val proximoMantenimientoKm: Int
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
    val resuelta: Boolean
)

data class MensajeChat(
    val id: Long,
    val autor: String,
    val texto: String,
    val hora: String,
    val esPropio: Boolean
)

data class ConversacionResumen(
    val id: Long,
    val nombreConductor: String,
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int
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
