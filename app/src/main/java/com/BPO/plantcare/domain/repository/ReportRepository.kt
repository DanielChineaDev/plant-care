package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.Report
import com.BPO.plantcare.domain.model.ReportReason
import com.BPO.plantcare.domain.model.ReportStatus
import com.BPO.plantcare.domain.model.ReportedContentType
import kotlinx.coroutines.flow.Flow

interface ReportRepository {

    /** Cola de reportes pendientes (solo admins pueden leer). */
    fun observePendingReports(): Flow<List<Report>>

    suspend fun submitReport(
        contentType: ReportedContentType,
        communityId: String,
        postId: String?,
        commentId: String?,
        reason: ReportReason,
        notes: String?,
    ): Result<String>

    /** Marca un reporte como revisado (dismiss o action). */
    suspend fun resolveReport(reportId: String, newStatus: ReportStatus): Result<Unit>
}
