package com.example.prueba.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmpresaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(empresa: EmpresaEntity)

    @Query("SELECT * FROM empresa WHERE id = :id LIMIT 1")
    suspend fun obtener(id: String): EmpresaEntity?
}
