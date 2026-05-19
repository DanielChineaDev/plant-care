package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.GlobalSearchResult
import com.BPO.plantcare.domain.repository.GlobalSearchRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Busqueda global en Firestore con prefix-match case-insensitive sobre
 * `nameLower` (comunidades) y `displayNameLower` (usuarios).
 *
 * Los campos *Lower los escribe la app al crear / editar el doc. Para
 * docs antiguos sin esos campos, ejecutar la Cloud Function HTTP
 * `backfillLowercaseFields` (definida en functions/index.js) UNA vez,
 * desde Firebase Console o gcloud. Documentado en el README.
 */
@Singleton
class GlobalSearchRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : GlobalSearchRepository {

    override suspend fun search(query: String, limitPerType: Int): Result<List<GlobalSearchResult>> =
        runCatching {
            val q = query.trim().lowercase()
            if (q.length < 2) return@runCatching emptyList()
            // Prefix-match clasico: cota superior con el codepoint mas alto
            // que Firestore tolera. Se anade al final del prefix.
            val end = q + ""

            val communitiesTask = firestore.collection(COMMUNITIES)
                .orderBy("nameLower")
                .startAt(q)
                .endAt(end)
                .limit(limitPerType.toLong())
                .get()
            val usersTask = firestore.collection(USERS)
                .orderBy("displayNameLower")
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
