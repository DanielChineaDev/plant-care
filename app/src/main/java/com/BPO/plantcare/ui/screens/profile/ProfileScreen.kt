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
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                ProfileEvent.LocationSaved -> "Ubicacion guardada"
                ProfileEvent.LocationUnavailable ->
                    "No se pudo obtener la ubicacion. Abre Google Maps un momento o activa GPS y reintenta."
                is ProfileEvent.SignInFailed -> "Error de login: ${event.message}"
                ProfileEvent.SignedOut -> "Sesion cerrada"
                is ProfileEvent.PublicToggled ->
                    if (event.enabled) "Coleccion publicada" else "Coleccion privada"
                ProfileEvent.Resynced -> "Coleccion publica resincronizada"
                is ProfileEvent.PublicError -> "Error: ${event.message}"
                is ProfileEvent.BackupExported ->
                    "Backup creado con ${event.plantCount} plantas"
                is ProfileEvent.BackupFailed -> "Error en backup: ${event.message}"
                is ProfileEvent.BackupImported ->
                    "Importadas ${event.plantCount} plantas desde el backup"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuracion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
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
                text = "Cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Cerrar sesion")
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
                    text = "Clima",
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
                    Text("Saltar riego si llovio", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Para plantas marcadas como exterior, omitimos su recordatorio si han caido al menos 5 mm de lluvia en las ultimas 24h en tu zona.",
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
                        Text("Conceder permiso de ubicacion")
                    }
                } else {
                    Text(
                        text = if (settings.hasLocation)
                            "Ubicacion guardada: ${"%.3f".format(settings.latitude)}, " +
                                    "${"%.3f".format(settings.longitude)}"
                        else "Sin ubicacion guardada.",
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
                            Text(if (settings.hasLocation) "Actualizar" else "Obtener")
                        }
                        if (settings.hasLocation) {
                            TextButton(onClick = onClearLocation) { Text("Borrar") }
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
        com.BPO.plantcare.domain.model.Season.Winter -> "invierno (riega 50% menos)"
        com.BPO.plantcare.domain.model.Season.Summer -> "verano (riega 15% mas)"
        com.BPO.plantcare.domain.model.Season.SpringOrAutumn -> "primavera/otono (sin ajuste)"
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
                    text = "Ajuste estacional del riego",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Adaptar riego segun estacion", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Hoy estamos en $seasonLabel. El intervalo guardado en cada planta NO cambia; solo cuando toca la notif diaria.",
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
                text = "Notificaciones",
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
                    Text("Recordatorio diario de riego", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Te avisamos cada manana de las plantas que toca regar.",
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
                Text("Probar notificacion de riego")
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
                    text = "Modo viaje",
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
                    Text("Pausar notificaciones", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Mientras estes de viaje no recibiras avisos de riego.",
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
                    OutlinedButton(
                        onClick = { pickerOpen = TravelPicker.Start },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Desde: ${formatOrPlaceholder(settings.travelStart)}")
                    }
                    OutlinedButton(
                        onClick = { pickerOpen = TravelPicker.End },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Hasta: ${formatOrPlaceholder(settings.travelEnd)}")
                    }
                }
                if (settings.isCurrentlyOnTrip()) {
                    Text(
                        text = "✈ Estas de viaje ahora. Las notificaciones estan pausadas.",
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
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = null }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private enum class TravelPicker { Start, End }

private fun formatOrPlaceholder(timestamp: Long?): String =
    timestamp?.let { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it)) }
        ?: "elegir"

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
            label = { Text("Hora del recordatorio") },
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
                    text = "Mi coleccion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hacer publica mi coleccion", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Otros usuarios podran ver tus plantas tocando tu nombre en cualquier post.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isPublic, onCheckedChange = onToggle)
            }
            if (isPublic) {
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedButton(onClick = onResync, modifier = Modifier.fillMaxWidth()) {
                    Text("Resincronizar coleccion publica")
                }
                Text(
                    text = "Tras anadir/borrar plantas, pulsa aqui para reflejar los cambios.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

