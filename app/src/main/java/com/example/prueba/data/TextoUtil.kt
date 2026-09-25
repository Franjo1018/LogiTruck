package com.example.prueba.data

import java.text.Normalizer

/**
 * Convierte un nombre completo en un nombre de usuario simple (minúsculas, sin espacios ni
 * tildes) para que la persona no tenga que inventarse uno al registrarse — pensado sobre todo
 * para choferes que prefieren no lidiar con un correo o un usuario que tengan que recordar
 * aparte de su propio nombre.
 *
 * Se usa tanto en RegistroScreen (para mostrar la sugerencia en vivo mientras la persona
 * escribe su nombre) como en AuthRepository (al crear la cuenta de verdad), así los dos lados
 * generan lo mismo a partir del mismo nombre.
 */
fun sugerirUsuarioDesdeNombre(nombreCompleto: String): String {
    val sinTildes = Normalizer.normalize(nombreCompleto.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    val soloLetras = sinTildes.lowercase().filter { it.isLetter() }
    return soloLetras.take(20).ifBlank { "usuario" }
}
