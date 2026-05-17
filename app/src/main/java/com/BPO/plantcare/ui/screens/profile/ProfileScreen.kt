package com.BPO.plantcare.ui.screens.profile

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
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.domain.repository.AuthState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileScreen(
    onOpenLightMeter: () -> Unit,
    onOpenDiagnosis: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                ProfileEvent.LocationSaved -> "Ubicacion guardada"
                ProfileEvent.LocationUnavailable ->
                    "No se pudo obtener la ubicacion. Abre Google Maps un momento o activa GPS y reintenta."
                is ProfileEvent.SignInFailed -> "Error de login: ${event.message}"
                ProfileEvent.SignedOut -> "Sesion cerrada"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        AuthHeaderCard(
            authState = authState,
            onSignIn = { viewModel.signInWithGoogle(context) },
            onSignOut = { viewModel.signOut() },
        )

        NotificationsCard(
            settings = settings,
            onToggle = viewModel::setNotificationsEnabled,
            onHourChange = viewModel::setReminderHour,
            onTest = viewModel::testWateringNotification,
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

        ToolsCard(
            onOpenLightMeter = onOpenLightMeter,
            onOpenDiagnosis = onOpenDiagnosis,
        )
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
private fun ToolsCard(
    onOpenLightMeter: () -> Unit,
    onOpenDiagnosis: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Herramientas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedButton(onClick = onOpenLightMeter, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.LightMode, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Medir luz de un sitio")
            }
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedButton(onClick = onOpenDiagnosis, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Diagnostico de plagas y enfermedades")
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
private fun AuthHeaderCard(
    authState: AuthState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (authState) {
                AuthState.Loading -> CircularProgressIndicator(modifier = Modifier.size(40.dp))

                AuthState.SignedOut -> {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Inicia sesion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "Sincroniza tus plantas, unete a comunidades y comparte tu coleccion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Button(onClick = onSignIn) {
                        Text("Iniciar sesion con Google")
                    }
                }

                is AuthState.SignedIn -> {
                    val photo = authState.profile.photoUrl
                    if (photo != null) {
                        AsyncImage(
                            model = photo,
                            contentDescription = authState.profile.displayName,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        text = authState.profile.displayName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    authState.profile.email?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedButton(onClick = onSignOut) { Text("Cerrar sesion") }
                }
            }
        }
    }
}
