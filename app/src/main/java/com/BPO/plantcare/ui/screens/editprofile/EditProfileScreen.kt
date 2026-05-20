package com.BPO.plantcare.ui.screens.editprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.core.storage.copyUriToCache

/**
 * Pantalla "Editar perfil". Tres secciones:
 *   - Foto (avatar): tap para elegir de galeria; sube a Storage y refresca.
 *   - Nombre: campo de texto + boton Guardar.
 *   - Contrasena: dos campos (nueva, confirmar) + boton Cambiar.
 *
 * Cambiar contrasena puede fallar con "requires-recent-login" si el user
 * no se logueo recientemente; mostramos el error humanizado y le decimos
 * que vuelva a loguearse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var name by rememberSaveable(profile?.displayName) {
        mutableStateOf(profile?.displayName.orEmpty())
    }
    var bio by rememberSaveable(profile?.bio) { mutableStateOf(profile?.bio.orEmpty()) }
    var location by rememberSaveable(profile?.location) {
        mutableStateOf(profile?.location.orEmpty())
    }
    var favorites by rememberSaveable(profile?.favoritePlants) {
        mutableStateOf(profile?.favoritePlants?.joinToString(", ").orEmpty())
    }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val file = copyUriToCache(context, uri)
            viewModel.uploadAvatar(file)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                EditProfileEvent.NameSaved -> "Nombre actualizado"
                EditProfileEvent.PhotoSaved -> "Foto actualizada"
                EditProfileEvent.PasswordChanged -> {
                    password = ""
                    confirmPassword = ""
                    "Contrasena actualizada"
                }
                EditProfileEvent.DetailsSaved -> "Perfil actualizado"
                is EditProfileEvent.Error -> event.message
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvatarCard(
                photoUrl = profile?.photoUrl,
                uploading = state.uploadingPhoto,
                onPickPhoto = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
            )

            NameCard(
                name = name,
                onNameChange = { name = it },
                saving = state.savingName,
                onSave = { viewModel.saveName(name) },
            )

            DetailsCard(
                bio = bio,
                location = location,
                favorites = favorites,
                onBioChange = { bio = it },
                onLocationChange = { location = it },
                onFavoritesChange = { favorites = it },
                saving = state.savingDetails,
                onSave = {
                    viewModel.saveDetails(
                        bio = bio,
                        location = location,
                        favoritePlants = favorites.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() },
                    )
                },
            )

            BadgesVisibilityCard(
                isPublic = profile?.badgesPublic ?: true,
                onToggle = viewModel::setBadgesPublic,
            )

            PasswordCard(
                password = password,
                confirm = confirmPassword,
                onPasswordChange = { password = it },
                onConfirmChange = { confirmPassword = it },
                passwordVisible = passwordVisible,
                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                changing = state.changingPassword,
                onSubmit = {
                    if (password == confirmPassword) {
                        viewModel.changePassword(password)
                    }
                },
            )
        }
    }
}

@Composable
private fun AvatarCard(
    photoUrl: String?,
    uploading: Boolean,
    onPickPhoto: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(120.dp),
                    )
                }
                if (uploading) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = onPickPhoto, enabled = !uploading) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Cambiar foto")
            }
        }
    }
}

@Composable
private fun NameCard(
    name: String,
    onNameChange: (String) -> Unit,
    saving: Boolean,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nombre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                label = { Text("Como te llamas") },
            )
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = onSave,
                enabled = !saving && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else Text("Guardar nombre")
            }
        }
    }
}

@Composable
private fun BadgesVisibilityCard(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Insignias publicas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Permite que otros usuarios vean tus logros en tu perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Switch(checked = isPublic, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun DetailsCard(
    bio: String,
    location: String,
    favorites: String,
    onBioChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onFavoritesChange: (String) -> Unit,
    saving: Boolean,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sobre ti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) onBioChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Biografia") },
                placeholder = { Text("Cuentanos algo sobre ti") },
                minLines = 2,
                maxLines = 4,
                supportingText = { Text("${bio.length}/160") },
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Localizacion (opcional)") },
                placeholder = { Text("Ej. Madrid") },
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = favorites,
                onValueChange = onFavoritesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Plantas favoritas") },
                placeholder = { Text("Separadas por comas: Monstera, Pothos") },
                minLines = 1,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else Text("Guardar perfil")
            }
        }
    }
}

@Composable
private fun PasswordCard(
    password: String,
    confirm: String,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    changing: Boolean,
    onSubmit: () -> Unit,
) {
    val mismatch = confirm.isNotEmpty() && password != confirm
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cambiar contrasena",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Si te logueaste con Google no hace falta tener contrasena. Si la cambias, podras entrar tambien con email + contrasena.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                label = { Text("Nueva contrasena") },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = onConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                label = { Text("Confirmar contrasena") },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                isError = mismatch,
                supportingText = {
                    if (mismatch) Text("Las contrasenas no coinciden")
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = onSubmit,
                enabled = !changing && password.length >= 6 && password == confirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (changing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else Text("Cambiar contrasena")
            }
        }
    }
}
