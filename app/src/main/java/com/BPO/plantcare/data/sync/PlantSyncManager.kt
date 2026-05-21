package com.BPO.plantcare.data.sync

import android.content.Context
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.dao.PlantPhotoDao
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincroniza las plantas locales (Room) con su espejo en Firestore por
 * usuario. Modelo:
 *  - Las escrituras locales se empujan a la nube (en PlantRepositoryImpl).
 *  - Al iniciar sesion, este manager concilia segun el caso:
 *      * Mismo usuario que la ultima vez: añade las plantas de la nube que no
 *        existan en local (altas hechas en otro dispositivo), sin pisar las
 *        locales.
 *      * Primer sync de este usuario en el dispositivo: si la nube tiene
 *        plantas, manda la nube (reemplaza local); si no, sube lo local.
 *      * Cambio de cuenta: limpia local y baja las plantas de la cuenta nueva.
 *
 * Ademas de las plantas, baja los metadatos de la galeria de fotos de cada
 * planta (con su URL en Storage) para que el diario fotografico sobreviva al
 * cambio de cuenta/dispositivo. No se sincroniza el historial de riego ni las
 * tareas.
 */
@Singleton
class PlantSyncManager @Inject constructor(
    @ApplicationContext context: Context,
    private val plantDao: PlantDao,
    private val plantPhotoDao: PlantPhotoDao,
    private val cloud: PlantCloudDataSource,
    private val photoCloud: PlantPhotoCloudDataSource,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    @Volatile private var started = false

    /** Arranca el observador de sesion (idempotente). */
    fun start(scope: CoroutineScope, authRepository: AuthRepository) {
        if (started) return
        started = true
        scope.launch {
            authRepository.authState
                .map { (it as? AuthState.SignedIn)?.profile?.uid }
                .distinctUntilChanged()
                .collect { uid -> if (uid != null) runCatching { sync(uid) } }
        }
    }

    private suspend fun sync(uid: String) {
        val last = prefs.getString(KEY_LAST_UID, null)
        val cloudPlants = cloud.fetchAll(uid)
        when {
            last == uid -> {
                val localIds = plantDao.getAll().map { it.id }.toSet()
                cloudPlants.filter { it.id !in localIds }
                    .forEach { plantDao.insert(it.toEntity()) }
            }
            last == null -> {
                val local = plantDao.getAll()
                if (cloudPlants.isNotEmpty()) {
                    plantDao.deleteAll()
                    cloudPlants.forEach { plantDao.insert(it.toEntity()) }
                } else if (local.isNotEmpty()) {
                    local.forEach { cloud.upsert(uid, it.toDomain()) }
                }
            }
            else -> {
                // Cambio de cuenta.
                plantDao.deleteAll()
                cloudPlants.forEach { plantDao.insert(it.toEntity()) }
            }
        }
        // Baja la galeria de fotos: para cada planta de la nube, inserta los
        // metadatos de fotos que no existan ya en local (la imagen se carga
        // por URL desde Storage; no se descarga el fichero).
        val plantsToSync = plantDao.getAll().map { it.id }.toSet()
        for (plantId in plantsToSync) {
            runCatching {
                val localIds = plantPhotoDao.getForPlant(plantId).map { it.id }.toSet()
                photoCloud.fetchForPlant(uid, plantId)
                    .filter { it.id !in localIds }
                    .forEach { plantPhotoDao.insert(it.toEntity()) }
            }
        }
        prefs.edit().putString(KEY_LAST_UID, uid).apply()
    }

    companion object {
        private const val PREFS = "plant_sync"
        private const val KEY_LAST_UID = "last_uid"
    }
}
