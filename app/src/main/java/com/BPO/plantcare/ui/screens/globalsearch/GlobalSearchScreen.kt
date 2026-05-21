package com.BPO.plantcare.ui.screens.globalsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.GlobalSearchResult

/**
 * Pantalla de busqueda global. Campo de busqueda en la TopAppBar,
 * resultados mezclados (especies + comunidades + usuarios) y al tocar
 * navegamos al destino correspondiente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onSpeciesClick: (scientificName: String) -> Unit,
    onCommunityClick: (communityId: String) -> Unit,
    onUserClick: (uid: String) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                placeholder = { Text(stringResource(R.string.globalsearch_placeholder)) },
            )

            when {
                query.length < 2 -> Hint(
                    text = stringResource(R.string.globalsearch_hint_short),
                )
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                results.isEmpty() -> Hint(
                    text = stringResource(R.string.globalsearch_no_results, query),
                )
                else -> ResultsList(
                    results = results,
                    onSpeciesClick = onSpeciesClick,
                    onCommunityClick = onCommunityClick,
                    onUserClick = onUserClick,
                )
            }
        }
    }
}

@Composable
private fun ResultsList(
    results: List<GlobalSearchResult>,
    onSpeciesClick: (String) -> Unit,
    onCommunityClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
) {
    val grouped = results.groupBy { it::class }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val species = grouped[GlobalSearchResult.Species::class].orEmpty()
        val communities = grouped[GlobalSearchResult.CommunityResult::class].orEmpty()
        val users = grouped[GlobalSearchResult.UserResult::class].orEmpty()

        if (species.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.globalsearch_section_species)) }
            items(species, key = { it.id }) { r ->
                ResultRow(result = r, onClick = { onSpeciesClick((r as GlobalSearchResult.Species).scientificName) })
            }
        }
        if (communities.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.globalsearch_section_communities)) }
            items(communities, key = { it.id }) { r ->
                ResultRow(result = r, onClick = { onCommunityClick(r.id) })
            }
        }
        if (users.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.globalsearch_section_users)) }
            items(users, key = { it.id }) { r ->
                ResultRow(result = r, onClick = { onUserClick(r.id) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultRow(result: GlobalSearchResult, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(result)
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                result.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar(result: GlobalSearchResult) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (result.imageUrl != null) {
            AsyncImage(
                model = result.imageUrl,
                contentDescription = result.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val icon: ImageVector = when (result) {
                is GlobalSearchResult.Species -> Icons.Outlined.LocalFlorist
                is GlobalSearchResult.CommunityResult -> Icons.Outlined.Groups
                is GlobalSearchResult.UserResult -> Icons.Outlined.Person
            }
            val color = MaterialTheme.colorScheme.primaryContainer
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                )
                if (result is GlobalSearchResult.CommunityResult) {
                    Text(text = result.emoji, fontSize = 22.sp)
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                @Suppress("UNUSED_EXPRESSION") color
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@Suppress("unused")
private val UNUSED_CIRCLE = CircleShape
