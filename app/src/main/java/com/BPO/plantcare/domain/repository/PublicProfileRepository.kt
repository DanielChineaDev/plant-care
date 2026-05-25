package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface PublicProfileRepository {
    /** Observa el perfil publico del usuario [uid] en Firestore. */
    fun observeUserProfile(uid: String): Flow<UserProfile?>

    /** Plantas publicas del usuario [uid] (vacio si la coleccion es privada). */
    fun observePublicPlants(uid: String): Flow<List<PublicPlant>>

    /** Observa una sola planta publica del usuario [uid]. */
    fun observePublicPlant(uid: String, plantId: String): Flow<PublicPlant?>

    /**
     * Cambia el flag isCollectionPublic del usuario actual.
     * - Si pasa a true: sube todas las plantas locales como public mirror.
     * - Si pasa a false: borra todo el mirror.
     */
    suspend fun setMyCollectionPublic(enabled: Boolean): Result<Unit>

    /** Resincroniza el mirror publico con las plantas locales actuales. */
    suspend fun resyncMyPublicCollection(): Result<Unit>

    /** Mapa logroId -> fecha de desbloqueo (epoch millis) del usuario [uid]. */
    fun observeAchievements(uid: String): Flow<Map<String, Long>>

    /** Registra un logro conseguido por el usuario actual (si no existia). */
    suspend fun recordAchievement(achievementId: String): Result<Unit>

    /** Cambia la visibilidad publica de las insignias del usuario actual. */
    suspend fun setBadgesPublic(enabled: Boolean): Result<Unit>

    /** Visibilidad del diario fotografico en el detalle publico de plantas. */
    suspend fun setDiaryPublic(enabled: Boolean): Result<Unit>

    /** Visibilidad de las notas/descripcion en el detalle publico de plantas. */
    suspend fun setNotesPublic(enabled: Boolean): Result<Unit>

    /** Visibilidad de la info basica de cuidados en el detalle publico. */
    suspend fun setCareInfoPublic(enabled: Boolean): Result<Unit>
}
