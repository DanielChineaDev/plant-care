package com.BPO.plantcare.ui.screens.diagnosis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.BPO.plantcare.domain.model.DiagnosisSeverity
import com.BPO.plantcare.domain.model.PlantDiagnosis
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisDetailScreen(
    onBack: () -> Unit,
    viewModel: DiagnosisDetailViewModel = hiltViewModel(),
) {
    val diagnosis = viewModel.diagnosis

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(diagnosis?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (diagnosis == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Diagnostico no encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderCard(diagnosis)
            SectionCard(
                icon = Icons.Outlined.Visibility,
                title = "Sintomas",
                items = diagnosis.symptoms,
            )
            SectionCard(
                icon = Icons.Outlined.WarningAmber,
                title = "Causas frecuentes",
                items = diagnosis.causes,
            )
            SectionCard(
                icon = Icons.Outlined.Healing,
                title = "Tratamiento",
                items = diagnosis.treatment,
                accent = true,
            )
            SectionCard(
                icon = Icons.Outlined.Shield,
                title = "Prevencion",
                items = diagnosis.prevention,
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HeaderCard(diagnosis: PlantDiagnosis) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconFor(diagnosis.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = diagnosis.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    diagnosis.scientificName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(diagnosis.category.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
                val sevColor = when (diagnosis.severity) {
                    DiagnosisSeverity.LOW -> StatusHealthy
                    DiagnosisSeverity.MEDIUM -> StatusWarning
                    DiagnosisSeverity.HIGH -> StatusThirsty
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(diagnosis.severity.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = sevColor.copy(alpha = 0.9f),
                        disabledLabelColor = Color.White,
                    ),
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = diagnosis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    items: List<String>,
    accent: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (accent) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (accent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (accent) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            items.forEachIndexed { idx, item ->
                BulletRow(text = item, accent = accent, numbered = accent, number = idx + 1)
            }
        }
    }
}

@Composable
private fun BulletRow(
    text: String,
    accent: Boolean,
    numbered: Boolean,
    number: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (numbered) {
            Text(
                text = "$number.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.width(20.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (accent) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}
