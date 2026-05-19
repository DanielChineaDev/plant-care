package com.BPO.plantcare.ui.screens.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Report
import com.BPO.plantcare.domain.model.ReportStatus
import com.BPO.plantcare.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val repository: ReportRepository,
) : ViewModel() {

    val reports: StateFlow<List<Report>> = repository.observePendingReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun dismiss(id: String) {
        viewModelScope.launch { repository.resolveReport(id, ReportStatus.Dismissed) }
    }

    fun markActioned(id: String) {
        viewModelScope.launch { repository.resolveReport(id, ReportStatus.Actioned) }
    }
}
