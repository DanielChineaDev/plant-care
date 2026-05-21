package com.BPO.plantcare.ui.screens.diagnosephoto

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.R
import com.BPO.plantcare.core.storage.copyUriToCache
import com.BPO.plantcare.domain.model.DiagnosisMatch
import java.io.File

/**
 * Pantalla para diagnosticar una planta a partir de una foto. Por ahora
 * el repo es un STUB que devuelve matches aleatorios del catalogo local;
 * la pantalla lo indica con un banner "modo demo".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosePhotoScreen(
    onBack: () -> Unit,
    onDiagnosisClick: (id: String) -> Unit,
    viewModel: DiagnosePhotoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val file = copyUriToCache(context, uri)
            viewModel.analyze(file)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dphoto_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DemoBanner()

            state.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.dphoto_photo_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Button(
                onClick = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (state.photoPath == null) stringResource(R.string.dphoto_pick) else stringResource(R.string.dphoto_change))
            }

            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.dphoto_analyzing), style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.error != null -> Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.analysis != null -> ResultsList(
                    matches = state.analysis!!.matches,
                    onClick = onDiagnosisClick,
                )
                else -> Hint()
            }
        }
    }
}

@Composable
private fun DemoBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.HealthAndSafety,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.dphoto_demo),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ResultsList(
    matches: List<DiagnosisMatch>,
    onClick: (String) -> Unit,
) {
    if (matches.isEmpty()) {
        Text(
            text = stringResource(R.string.dphoto_no_matches),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(matches, key = { it.diagnosis.id }) { match ->
            Card(
                onClick = { onClick(match.diagnosis.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = match.diagnosis.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.dphoto_confidence, (match.confidence * 100).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    LinearProgressIndicator(
                        progress = { match.confidence },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.dphoto_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
