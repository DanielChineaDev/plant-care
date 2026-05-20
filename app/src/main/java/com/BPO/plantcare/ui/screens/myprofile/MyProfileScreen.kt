package com.BPO.plantcare.ui.screens.myprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.domain.model.Achievement
import com.BPO.plantcare.domain.model.ProfileStats
import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile

/**
 * Pantalla "Mi perfil" (drawer). Muestra el perfil publico del usuario
 * actual + sus plantas publicas. Sirve para que el dueno vea como ven los
 * demas su coleccion.
 *
 * Si la coleccion esta marcada como privada, muestra un aviso indicando que
 * los demas no la veran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val plants by viewModel.publicPlants.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        val current = profile
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    ProfileHeader(profile = current, onEditClick = onEditProfile)
                    Spacer(modifier = Modifier.size(12.dp))
                    StatsCard(stats = stats)
                    Spacer(modifier = Modifier.size(12.dp))
                    AchievementsSection(achievements = achievements)
                    if (!current.isCollectionPublic) {
                        Spacer(modifier = Modifier.size(12.dp))
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Tu coleccion esta marcada como privada. Activa el switch en Configuracion -> Mi coleccion para que otros vean tus plantas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "${plants.size} plantas en tu coleccion publica",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (plants.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = if (current.isCollectionPublic)
                            "Aun no tienes plantas publicadas. Resincronizalas desde Configuracion."
                        else
                            "Coleccion privada: anade plantas y publica la coleccion para verlas aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            } else {
                items(plants, key = { it.id }) { plant ->
                    PublicPlantCard(plant)
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile, onEditClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (profile.photoUrl != null) {
                AsyncImage(
                    model = profile.photoUrl,
                    contentDescription = profile.displayName,
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = profile.displayName ?: "Usuario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            profile.email?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            profile.location?.takeIf { it.isNotBlank() }?.let { loc ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = loc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (profile.favoritePlants.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(profile.favoritePlants) { fav ->
                        AssistChip(
                            onClick = {},
                            label = { Text("🌿 $fav") },
                        )
                    }
                }
            }
            Text(
                text = "Karma: ${profile.karma}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Editar perfil")
            }
        }
    }
}

@Composable
private fun StatsCard(stats: ProfileStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estadisticas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile("🪴", stats.plantCount.toString(), "Plantas", Modifier.weight(1f))
                StatTile("✍️", stats.postCount.toString(), "Posts", Modifier.weight(1f))
                StatTile("💬", stats.commentCount.toString(), "Comentarios", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile("💧", stats.totalWaterings.toString(), "Riegos", Modifier.weight(1f))
                StatTile("🔥", stats.wateringStreak.toString(), "Racha (d)", Modifier.weight(1f))
                StatTile("📅", stats.memberSinceDays.toString(), "Dias aqui", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    if (achievements.isEmpty()) return
    val unlocked = achievements.count { it.unlocked }
    Column {
        Text(
            text = "Logros ($unlocked/${achievements.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(achievements) { ach ->
                AchievementBadge(ach)
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (achievement.unlocked) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(width = 96.dp, height = 110.dp)
            .alpha(if (achievement.unlocked) 1f else 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = achievement.emoji, fontSize = 30.sp)
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PublicPlantCard(plant: PublicPlant) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (plant.referenceImageUrl != null) {
                    AsyncImage(
                        model = plant.referenceImageUrl,
                        contentDescription = plant.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.LocalFlorist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = plant.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = plant.scientificName,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
