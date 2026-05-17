package com.BPO.plantcare.ui.screens.diagnosis

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Coronavirus
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.domain.model.DiagnosisCategory
import com.BPO.plantcare.domain.model.DiagnosisSeverity
import com.BPO.plantcare.domain.model.PlantDiagnosis
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisListScreen(
    onBack: () -> Unit,
    onDiagnosisClick: (String) -> Unit,
    viewModel: DiagnosisListViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostico") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchBar(query = filters.query, onQueryChange = viewModel::onQueryChange)
            CategoryFilters(
                selected = filters.category,
                onToggle = viewModel::toggleCategory,
                onClear = viewModel::clearAll,
            )
            if (results.isEmpty()) {
                EmptyResults(filters.query)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { diagnosis ->
                        DiagnosisCard(
                            diagnosis = diagnosis,
                            onClick = { onDiagnosisClick(diagnosis.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Busca por nombre o sintoma") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun CategoryFilters(
    selected: DiagnosisCategory?,
    onToggle: (DiagnosisCategory) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiagnosisCategory.entries.forEach { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onToggle(c) },
                label = { Text(c.label) },
            )
        }
        if (selected != null) {
            TextButton(onClick = onClear) { Text("Limpiar") }
        }
    }
}

@Composable
private fun DiagnosisCard(diagnosis: PlantDiagnosis, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconFor(diagnosis.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                SeverityChip(diagnosis.severity)
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = diagnosis.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = diagnosis.category.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = diagnosis.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeverityChip(severity: DiagnosisSeverity) {
    val color = when (severity) {
        DiagnosisSeverity.LOW -> StatusHealthy
        DiagnosisSeverity.MEDIUM -> StatusWarning
        DiagnosisSeverity.HIGH -> StatusThirsty
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(severity.label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.85f),
            disabledLabelColor = Color.White,
        ),
    )
}

@Composable
private fun EmptyResults(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (query.isBlank()) "Sin coincidencias"
            else "Sin resultados para \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

internal fun iconFor(category: DiagnosisCategory): ImageVector = when (category) {
    DiagnosisCategory.PEST -> Icons.Outlined.BugReport
    DiagnosisCategory.FUNGAL -> Icons.Outlined.Science
    DiagnosisCategory.BACTERIAL -> Icons.Outlined.Coronavirus
    DiagnosisCategory.VIRAL -> Icons.Outlined.HealthAndSafety
    DiagnosisCategory.PHYSIOLOGICAL -> Icons.Outlined.WarningAmber
}
