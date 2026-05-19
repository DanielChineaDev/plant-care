package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.Report
import com.BPO.plantcare.domain.model.ReportReason
import com.BPO.plantcare.domain.model.ReportStatus
import com.BPO.plantcare.domain.model.ReportedContentType
import com.BPO.plantcare.domain.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : ReportRepository {

    override fun observePendingReports(): Flow<List<Report>> = callbackFlow {
        val reg = firestore.collection(REPORTS)
            .whereEqualTo("status", ReportStatus.Pending.storageKey)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toReport() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun submitReport(
        contentType: ReportedContentType,
        communityId: String,
        postId: String?,
        commentId: String?,
        reason: ReportReason,
        notes: String?,
    ): Result<String> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("Inicia sesion para reportar")
        val doc = firestore.collection(REPORTS).document()
        val data = mapOf(
            "reporterUid" to uid,
            "contentType" to contentType.storageKey,
            "communityId" to communityId,
            "postId" to postId,
            "commentId" to commentId,
            "reason" to reason.storageKey,
            "notes" to notes,
            "status" to ReportStatus.Pending.storageKey,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        doc.set(data).await()
        doc.id
    }

    override suspend fun resolveReport(
        reportId: String,
        newStatus: ReportStatus,
    ): Result<Unit> = runCatching {
        firestore.collection(REPORTS).document(reportId)
            .update("status", newStatus.storageKey).await()
    }

    private fun DocumentSnapshot.toReport(): Report? {
        if (!exists()) return null
        return Report(
            id = id,
            reporterUid = getString("reporterUid").orEmpty(),
            contentType = ReportedContentType.fromKey(getString("contentType")),
            communityId = getString("communityId").orEmpty(),
            postId = getString("postId"),
            commentId = getString("commentId"),
            reason = ReportReason.fromKey(getString("reason")),
            notes = getString("notes"),
            status = ReportStatus.fromKey(getString("status")),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
        )
    }

    companion object {
        private const val REPORTS = "reports"
    }
}
