package com.example.killteam.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Image source for the measuring screen: the device camera, or a chosen photo on the desktop. */
interface CameraSource {
    /** Shown instead of the picture while the source is not ready (missing permission, no photo yet). */
    val message: String?

    /** Button under [message]. */
    val actionLabel: String?

    fun action()

    /** Optional button in the toolbar while the picture is shown (desktop: pick another photo). */
    val toolbarLabel: String?

    fun toolbarAction()

    /** The picture; only called while [message] is null. A frozen source shows a still image. */
    @Composable
    fun Content(modifier: Modifier, frozen: Boolean)
}

/** False where no camera or photo source is implemented; the measuring entry is then hidden. */
expect val cameraAvailable: Boolean

@Composable
expect fun rememberCameraSource(): CameraSource?
