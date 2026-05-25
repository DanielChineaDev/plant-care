package com.BPO.plantcare.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.ui.components.FeedPostCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onCommunitiesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onGlobalSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    onPostClick: (communityId: String, postId: String) -> Unit = { _, _ -> },
    onAuthorClick: (uid: String) -> Unit = {},
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(notifPermission.status) {
            if (notifPermission.status is PermissionStatus.Denied &&
                !(notifPermission.status as PermissionStatus.Denied).shouldShowRationale
            ) {
                notifPermission.launchPermissionRequest()
            }
        }
    }
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val hasJoined by viewModel.hasJoinedCommunities.collectAsStateWithLifecycle()
    val feedLoading by viewModel.feedLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val suggested by viewModel.suggestedCommunities.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.menu))
                    }
                },
                actions = {
                    IconButton(onClick = onGlobalSearchClick) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.home_search))
                    }
                    com.BPO.plantcare.ui.components.NotificationsActionButton(
                        onClick = onNotificationsClick,
                    )
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Padding bottom dentro del contentPadding para que los posts
            // puedan scrollearse pegados al bottom bar (sin hueco blanco).
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = padding.calculateBottomPadding() + 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FeedTabRow(
                    tab = tab,
                    onTabChange = viewModel::setTab,
                )
            }

            // En "Para ti" sin comunidades unidas mostramos el card de
            // sugerencias. En "Recientes"/"Mejor valoradas" mostramos el
            // feed de descubrimiento aunque no sigas ninguna comunidad.
            if (tab == FeedTab.ForYou && !hasJoined) {
                item {
                    SuggestedCommunitiesCard(
                        communities = suggested,
                        onJoin = viewModel::joinCommunity,
                        onSeeAll = onCommunitiesClick,
                        onCommunityClick = { /* abrir feed comunidad no aplica aqui */ },
                    )
                }
            } else if (feedLoading && feed.isEmpty()) {
                // Shimmer placeholders mientras llega la primera emision
                // de Firestore. Evita pantalla vacia y da feedback de
                // "estamos cargando algo".
                items(3) {
                    com.BPO.plantcare.ui.components.PostCardSkeleton()
                }
            } else if (feed.isEmpty()) {
                item {
                    EmptyFeedCard()
                }
            } else {
                items(feed, key = { it.post.id }) { item ->
                    FeedPostCard(
                        post = item.post,
                        communityName = item.community.name,
                        communityEmoji = item.community.emoji,
                        modifier = Modifier.animateItem(),
                        onClick = { onPostClick(item.community.id, item.post.id) },
                        onAuthorClick = onAuthorClick,
                        onLikeClick = { viewModel.toggleLike(item.community.id, item.post.id) },
                    )
                }
            }

        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTabRow(
    tab: FeedTab,
    onTabChange: (FeedTab) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = tab == FeedTab.ForYou,
            onClick = { onTabChange(FeedTab.ForYou) },
            label = { Text(stringResource(R.string.feed_tab_for_you)) },
        )
        FilterChip(
            selected = tab == FeedTab.Recent,
            onClick = { onTabChange(FeedTab.Recent) },
            label = { Text(stringResource(R.string.feed_tab_recent)) },
        )
        FilterChip(
            selected = tab == FeedTab.Top,
            onClick = { onTabChange(FeedTab.Top) },
            label = { Text(stringResource(R.string.feed_tab_top)) },
        )
    }
}

@Composable
private fun SuggestedCommunitiesCard(
    communities: List<Community>,
    onJoin: (String) -> Unit,
    onSeeAll: () -> Unit,
    onCommunityClick: (String) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_join_community_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.home_join_community_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))

            communities.forEach { community ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCommunityClick(community.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = community.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.members_count,
                                community.memberCount.toInt(),
                                community.memberCount,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { onJoin(community.id) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) { Text(stringResource(R.string.join)) }
                }
            }

            if (communities.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_communities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.size(8.dp))
            TextButton(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_see_all_communities))
            }
        }
    }
}

@Composable
private fun EmptyFeedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.home_empty_feed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

