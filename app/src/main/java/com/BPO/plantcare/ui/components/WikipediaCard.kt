package com.BPO.plantcare.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.WikipediaSummary
import com.BPO.plantcare.ui.screens.common.WikipediaUiState

@Composable
fun WikipediaCard(
    state: WikipediaUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.wiki_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (state) {
                is WikipediaUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.wiki_loading),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                is WikipediaUiState.Loaded -> WikipediaContent(
                    summary = state.summary,
                    onOpenLink = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    },
                )

                is WikipediaUiState.NotFound -> Text(
                    text = stringResource(R.string.wiki_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is WikipediaUiState.Error -> Text(
                    text = stringResource(R.string.wiki_error, state.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun WikipediaContent(summary: WikipediaSummary, onOpenLink: (String) -> Unit) {
    if (summary.thumbnailUrl != null) {
        AsyncImage(
            model = summary.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    Text(
        text = summary.extract,
        style = MaterialTheme.typography.bodyMedium,
    )
    if (summary.pageUrl != null) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { onOpenLink(summary.pageUrl) }) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.wiki_read_more, summary.lang))
        }
    }
}
