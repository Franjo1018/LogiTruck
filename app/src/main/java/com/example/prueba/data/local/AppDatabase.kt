package com.example.prueba.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local (Room/SQLite) del dispositivo. Ahora que la identidad la maneja
 * Firebase Authentication y los datos "de verdad" viven en Realtime Database organizados
 * por empresa, Room pasa a ser una caché offline del perfil del usuario/empresa de este
 * dispositivo (para que la app siga funcionando sin señal después del primer login).
 *
 * TODO: agregar el resto de entidades del ERD (VIAJE, UBICACION, VEHICULO, NOTIFICACION,
 * MENSAJE) a medida que se implementen sus pantallas.
 */
@Database(entities = [UsuarioEntity::class, EmpresaEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun empresaDao(): EmpresaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "logictruck.db"
                )
                    // El esquema cambió de forma incompatible (login pasó de Room a Firebase
                    // Auth). El proyecto todavía no tiene usuarios reales en producción, así
                    // que se acepta borrar la base local en vez de escribir una Migration.
                    // TODO: quitar esto y escribir Migrations reales antes de publicar la app.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
