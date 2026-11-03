package com.example.killteam.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner

actual val cameraAvailable: Boolean = true

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberCameraSource(): CameraSource? {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = hasCameraPermission(context)
        if (granted) denied = false
    }
    return remember(granted, denied) {
        object : CameraSource {
            override val message: String? = when {
                granted -> null
                denied -> "Camera access was denied. Allow it in the app settings to measure distances."
                else -> "Measuring needs the camera. The picture stays on this device and is not saved."
            }
            override val actionLabel: String? = when {
                granted -> null
                denied -> "Open settings"
                else -> "Allow camera"
            }

            override fun action() {
                if (denied) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            }

            override val toolbarLabel: String? = null

            override fun toolbarAction() = Unit

            @Composable
            override fun Content(modifier: Modifier, frozen: Boolean) = CameraPreview(modifier, frozen)
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier, frozen: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            keepScreenOn = true
        }
    }
    var still by remember { mutableStateOf<ImageBitmap?>(null) }

    // The camera is bound only while this view is shown and released when it leaves.
    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { if (future.isDone) future.get().unbindAll() } }
    }

    LaunchedEffect(frozen) { still = if (frozen) previewView.bitmap?.asImageBitmap() else null }

    AndroidView({ previewView }, modifier)
    still?.let { Image(it, null, modifier, contentScale = ContentScale.FillBounds) }
}
