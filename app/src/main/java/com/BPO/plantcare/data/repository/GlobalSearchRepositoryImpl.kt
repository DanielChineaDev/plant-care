package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.GlobalSearchResult
import com.BPO.plantcare.domain.repository.GlobalSearchRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Busqueda global en Firestore con prefix-match sobre `name` (comunidades)
 * y `displayName` (usuarios).
 *
 * Limitacion conocida: Firestore es case-sensitive, asi que buscar "ibex"
 * NO encontrara "Ibex35". En una iteracion futura, anadiriamos campos
 * `searchableName` lowercase al crear/editar el doc; por ahora aceptamos
 * el limite a cambio de no introducir migraciones masivas.
 */
@Singleton
class GlobalSearchRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : GlobalSearchRepository {

    override suspend fun search(query: String, limitPerType: Int): Result<List<GlobalSearchResult>> =
        runCatching {
            val q = query.trim()
            if (q.length < 2) return@runCatching emptyList()
            val end = q + ""

            // Para no pegarle a Firestore dos veces secuencialmente,
            // disparamos ambas queries en paralelo via await().
            val communitiesTask = firestore.collection(COMMUNITIES)
                .orderBy("name")
                .startAt(q)
                .endAt(end)
                .limit(limitPerType.toLong())
                .get()
            val usersTask = firestore.collection(USERS)
                .orderBy("displayName")
                .startAt(q)
                .endAt(end)
                .limit(limitPerType.toLong())
                .get()

            val communities = communitiesTask.await().documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                GlobalSearchResult.CommunityResult(
                    id = doc.id,
                    displayName = name,
                    subtitle = doc.getString("description"),
                    imageUrl = doc.getString("photoUrl"),
                    emoji = doc.getString("emoji") ?: "🌱",
                )
            }
            val users = usersTask.await().documents.mapNotNull { doc ->
                val name = doc.getString("displayName") ?: return@mapNotNull null
                GlobalSearchResult.UserResult(
                    id = doc.id,
                    displayName = name,
                    subtitle = null,
                    imageUrl = doc.getString("photoUrl"),
                )
            }
            communities + users
        }

    companion object {
        private const val COMMUNITIES = "communities"
        private const val USERS = "users"
    }
}
