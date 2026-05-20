package com.BPO.plantcare.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.Manifest
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.repository.AuthState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenLightMeter: () -> Unit,
    onOpenDiagnosis: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                ProfileEvent.LocationSaved ->
                    context.getString(R.string.settings_event_location_saved)
                ProfileEvent.LocationUnavailable ->
                    context.getString(R.string.settings_event_location_unavailable)
                is ProfileEvent.SignInFailed -> event.message
                ProfileEvent.SignedOut ->
                    context.getString(R.string.settings_event_signed_out)
                is ProfileEvent.PublicToggled ->
                    if (event.enabled) context.getString(R.string.settings_event_published)
                    else context.getString(R.string.settings_event_private)
                ProfileEvent.Resynced ->
                    context.getString(R.string.settings_event_resynced)
                is ProfileEvent.PublicError -> event.message
                is ProfileEvent.BackupExported -> event.plantCount.toString()
                is ProfileEvent.BackupFailed -> event.message
                is ProfileEvent.BackupImported -> event.plantCount.toString()
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeCard(
                settings = settings,
                onSelectPalette = viewModel::setThemePalette,
                onToggleDynamic = viewModel::setDynamicColor,
            )

            LanguageCard()

            NotificationsCard(
                settings = settings,
                onToggle = viewModel::setNotificationsEnabled,
                onHourChange = viewModel::setReminderHour,
                onTest = viewModel::testWateringNotification,
            )

            SeasonalAdjustCard(
                settings = settings,
                onToggle = viewModel::setSeasonalAdjustEnabled,
            )

            TravelModeCard(
                settings = settings,
                onToggle = viewModel::setTravelEnabled,
                onRangeChange = viewModel::setTravelRange,
            )

            WeatherCard(
                settings = settings,
                hasPermission = locationPermission.status is PermissionStatus.Granted,
                onToggle = viewModel::setWeatherAware,
                onRequestPermission = { locationPermission.launchPermissionRequest() },
                onRefreshLocation = viewModel::refreshLocation,
                onClearLocation = viewModel::clearLocation,
            )

            val signedInState = authState as? AuthState.SignedIn
            if (signedInState != null) {
                PublicCollectionCard(
                    isPublic = signedInState.profile.isCollectionPublic,
                    onToggle = viewModel::setCollectionPublic,
                    onResync = viewModel::resyncPublicCollection,
                )
            }

            SignOutCard(onSignOut = { viewModel.signOut() })

            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LanguageCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val current = com.BPO.plantcare.core.locale.LocaleHelper.getLanguage(context)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.settings_language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.BPO.plantcare.core.locale.LocaleHelper.SUPPORTED.forEach { (code, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = code == current,
                        onClick = {
                            if (code != current) {
                                com.BPO.plantcare.core.locale.LocaleHelper.setLanguage(context, code)
                                (context as? android.app.Activity)?.recreate()
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    settings: com.BPO.plantcare.domain.model.UserSettings,
    onSelectPalette: (String) -> Unit,
    onToggleDynamic: (Boolean) -> Unit,
) {
    val selected = com.BPO.plantcare.ui.theme.AppPalette.fromKey(settings.themePalette)
    val supportsDynamic = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.settings_theme_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.BPO.plantcare.ui.theme.AppPalette.entries.forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        selected = palette == selected && !settings.dynamicColor,
                        enabled = !settings.dynamicColor,
                        onClick = { onSelectPalette(palette.key) },
                    )
                }
            }
            if (supportsDynamic) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_dynamic_color),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_dynamic_color_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.dynamicColor, onCheckedChange = onToggleDynamic)
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: com.BPO.plantcare.ui.theme.AppPalette,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(44.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(palette.swatch)
                .then(
                    if (selected) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ) else Modifier
                ),
        )
        Text(
            text = stringResource(palette.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SignOutCard(onSignOut: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_account_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.sign_out))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherCard(
    settings: com.BPO.plantcare.domain.model.UserSettings,
    hasPermission: Boolean,
    onToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onRefreshLocation: () -> Unit,
    onClearLocation: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_weather_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_weather_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_weather_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.weatherAware, onCheckedChange = onToggle)
            }

            if (settings.weatherAware) {
                Spacer(modifier = Modifier.size(12.dp))
                if (!hasPermission) {
                    OutlinedButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_weather_grant))
                    }
                } else {
                    Text(
                        text = if (settings.hasLocation)
                            stringResource(
                                R.string.settings_weather_loc_saved,
                                "%.3f".format(settings.latitude),
                                "%.3f".format(settings.longitude),
                            )
                        else stringResource(R.string.settings_weather_no_loc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (settings.hasLocation) stringResource(R.string.settings_weather_update)
                                else stringResource(R.string.settings_weather_get),
                            )
                        }
                        if (settings.hasLocation) {
                            TextButton(onClick = onClearLocation) { Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonalAdjustCard(
    settings: com.BPO.plantcare.domain.model.UserSettings,
    onToggle: (Boolean) -> Unit,
) {
    val season = com.BPO.plantcare.domain.model.seasonOf()
    val seasonLabel = when (season) {
        com.BPO.plantcare.domain.model.Season.Winter -> stringResource(R.string.season_winter)
        com.BPO.plantcare.domain.model.Season.Summer -> stringResource(R.string.season_summer)
        com.BPO.plantcare.domain.model.Season.SpringOrAutumn ->
            stringResource(R.string.season_spring_autumn)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_seasonal_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_seasonal_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_seasonal_desc, seasonLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.seasonalAdjustEnabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsCard(
    settings: com.BPO.plantcare.domain.model.UserSettings,
    onToggle: (Boolean) -> Unit,
    onHourChange: (Int) -> Unit,
    onTest: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_notifications_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_notif_daily),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_notif_daily_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.notificationsEnabled, onCheckedChange = onToggle)
            }

            if (settings.notificationsEnabled) {
                Spacer(modifier = Modifier.size(12.dp))
                HourSelector(hour = settings.reminderHour, onHourChange = onHourChange)
            }

            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.settings_notif_test))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelModeCard(
    settings: com.BPO.plantcare.domain.model.UserSettings,
    onToggle: (Boolean) -> Unit,
    onRangeChange: (Long?, Long?) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf<TravelPicker?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Flight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_travel_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_travel_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_travel_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.travelEnabled, onCheckedChange = onToggle)
            }

            if (settings.travelEnabled) {
                Spacer(modifier = Modifier.size(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val choose = stringResource(R.string.settings_travel_choose)
                    OutlinedButton(
                        onClick = { pickerOpen = TravelPicker.Start },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_travel_from, formatOrPlaceholder(settings.travelStart, choose)))
                    }
                    OutlinedButton(
                        onClick = { pickerOpen = TravelPicker.End },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.settings_travel_to, formatOrPlaceholder(settings.travelEnd, choose)))
                    }
                }
                if (settings.isCurrentlyOnTrip()) {
                    Text(
                        text = stringResource(R.string.settings_travel_now),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    pickerOpen?.let { which ->
        val state = rememberDatePickerState(
            initialSelectedDateMillis = when (which) {
                TravelPicker.Start -> settings.travelStart ?: System.currentTimeMillis()
                TravelPicker.End -> settings.travelEnd ?: System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = null },
            confirmButton = {
                TextButton(onClick = {
                    val newMillis = state.selectedDateMillis
                    if (newMillis != null) {
                        if (which == TravelPicker.Start) {
                            onRangeChange(newMillis, settings.travelEnd)
                        } else {
                            onRangeChange(settings.travelStart, newMillis)
                        }
                    }
                    pickerOpen = null
                }) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = null }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private enum class TravelPicker { Start, End }

private fun formatOrPlaceholder(timestamp: Long?, placeholder: String): String =
    timestamp?.let { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it)) }
        ?: placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourSelector(hour: Int, onHourChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = formatHour(hour),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_notif_hour)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text = { Text(formatHour(h)) },
                    onClick = {
                        onHourChange(h)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatHour(hour: Int): String = "%02d:00".format(hour)

@Composable
private fun PublicCollectionCard(
    isPublic: Boolean,
    onToggle: (Boolean) -> Unit,
    onResync: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_public_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_public_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_public_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isPublic, onCheckedChange = onToggle)
            }
            if (isPublic) {
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedButton(onClick = onResync, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_public_resync))
                }
                Text(
                    text = stringResource(R.string.settings_public_resync_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

