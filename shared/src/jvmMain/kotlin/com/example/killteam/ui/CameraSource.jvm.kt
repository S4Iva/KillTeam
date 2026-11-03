package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.File
import javax.imageio.ImageIO

actual val cameraAvailable: Boolean = !GraphicsEnvironment.isHeadless()

private val photoExtensions = setOf("jpg", "jpeg", "png", "bmp", "gif")

private fun chooseImage(): Pair<ImageBitmap?, String?> {
    val dialog = FileDialog(null as Frame?, "Open photo of the table", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in photoExtensions }
    dialog.isVisible = true
    val name = dialog.file ?: return null to null
    val image = runCatching { ImageIO.read(File(dialog.directory, name)) }.getOrNull()
        ?: return null to "The file could not be read as an image."
    return image.toComposeImageBitmap() to null
}

/** Desktop has no camera access; the measuring screen works on a photo of the table instead. */
@Composable
actual fun rememberCameraSource(): CameraSource? {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    return remember(image, error) {
        object : CameraSource {
            override val message: String? = error ?: if (image == null) "Open a photo of the table to measure on it." else null
            override val actionLabel: String? = if (image == null) "Open photo" else null

            override fun action() = pick()

            override val toolbarLabel: String? = if (image != null) "Other photo" else null

            override fun toolbarAction() = pick()

            private fun pick() {
                val (picked, err) = chooseImage()
                if (picked != null) image = picked
                error = err
            }

            @Composable
            override fun Content(modifier: Modifier, frozen: Boolean) {
                image?.let { Image(it, null, modifier, contentScale = ContentScale.Fit) }
            }
        }
    }
}
