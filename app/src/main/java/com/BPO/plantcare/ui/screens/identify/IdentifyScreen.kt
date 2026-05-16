package com.BPO.plantcare.ui.screens.identify

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.BPO.plantcare.domain.model.PlantSuggestion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IdentifyScreen(
    onBack: () -> Unit,
    viewModel: IdentifyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (cameraPermission.status !is PermissionStatus.Granted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is IdentifyEvent.PlantAdded -> "${event.displayName} añadida a Mis plantas"
                is IdentifyEvent.AddFailed -> event.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identificar planta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val status = cameraPermission.status) {
            is PermissionStatus.Granted -> IdentifyContent(
                state = state,
                contentPadding = padding,
                onPhotoCaptured = viewModel::onPhotoCaptured,
                onIdentify = viewModel::identify,
                onRetake = viewModel::retake,
                onAddToMyPlants = viewModel::addSuggestionToMyPlants,
            )

            is PermissionStatus.Denied -> PermissionRationale(
                shouldShowRationale = status.shouldShowRationale,
                onRequest = { cameraPermission.launchPermissionRequest() },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun IdentifyContent(
    state: IdentifyUiState,
    contentPadding: PaddingValues,
    onPhotoCaptured: (Uri, File) -> Unit,
    onIdentify: () -> Unit,
    onRetake: () -> Unit,
    onAddToMyPlants: (PlantSuggestion) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        when (state) {
            is IdentifyUiState.Idle -> CameraView(onPhotoCaptured = onPhotoCaptured)

            is IdentifyUiState.Captured -> CapturedPreview(
                photoUri = state.photoUri,
                onIdentify = onIdentify,
                onRetake = onRetake,
            )

            is IdentifyUiState.Loading -> LoadingOverlay(state.photoUri)

            is IdentifyUiState.Success -> ResultsView(
                photoUri = state.photoUri,
                suggestions = state.suggestions,
                onRetake = onRetake,
                onAddToMyPlants = onAddToMyPlants,
            )

            is IdentifyUiState.Error -> ErrorView(
                photoUri = state.photoUri,
                message = state.message,
                onRetake = onRetake,
            )
        }
    }
}

@Composable
private fun CameraView(onPhotoCaptured: (Uri, File) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            bindToLifecycle(lifecycleOwner)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
        )

        FilledIconButton(
            onClick = { takePhoto(context, controller, onPhotoCaptured) },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(76.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Capturar foto",
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun CapturedPreview(
    photoUri: Uri,
    onIdentify: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = photoUri,
            contentDescription = "Foto capturada",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onIdentify,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Identificar") }
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text("  Repetir foto")
            }
        }
    }
}

@Composable
private fun LoadingOverlay(photoUri: Uri) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    text = "Identificando planta...",
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
    photoUri: Uri?,
    message: String,
    onRetake: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp),
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetake,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
        ) { Text("Probar de nuevo") }
    }
}

@Composable
private fun PermissionRationale(
    shouldShowRationale: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = if (shouldShowRationale) {
                "Necesitamos acceso a la camara para identificar tus plantas."
            } else {
                "Concede el permiso de camara para empezar a identificar plantas."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = onRequest,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
        ) { Text("Permitir camara") }
    }
}

private fun takePhoto(
    context: Context,
    controller: CameraController,
    onPhotoCaptured: (Uri, File) -> Unit,
) {
    val file = File(context.cacheDir, "plantcare_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    controller.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                val uri = results.savedUri ?: Uri.fromFile(file)
                onPhotoCaptured(uri, file)
            }

            override fun onError(exception: ImageCaptureException) {
                // Silent fail por ahora — la pantalla volvera al estado Idle.
            }
        },
    )
}
