package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.killteam.data.Archetype
import com.example.killteam.data.Killzone
import killteam.shared.generated.resources.Res
import killteam.shared.generated.resources.btn_nav_armylist
import killteam.shared.generated.resources.btn_nav_contents
import killteam.shared.generated.resources.btn_nav_totop
import killteam.shared.generated.resources.btn_remove
import killteam.shared.generated.resources.btn_select
import killteam.shared.generated.resources.icon_condition
import killteam.shared.generated.resources.icon_effect
import killteam.shared.generated.resources.tacop_destroy
import killteam.shared.generated.resources.tacop_infiltration
import killteam.shared.generated.resources.tacop_recon
import killteam.shared.generated.resources.tacop_security
import killteam.shared.generated.resources.key_a_drop
import killteam.shared.generated.resources.key_a_edge
import killteam.shared.generated.resources.key_a_obj
import killteam.shared.generated.resources.key_a_territory
import killteam.shared.generated.resources.key_b_drop
import killteam.shared.generated.resources.key_b_edge
import killteam.shared.generated.resources.key_b_obj
import killteam.shared.generated.resources.key_b_territory
import killteam.shared.generated.resources.key_centre
import killteam.shared.generated.resources.key_centreline
import killteam.shared.generated.resources.key_n_edge
import killteam.shared.generated.resources.key_n_obj
import killteam.shared.generated.resources.kill_grade
import killteam.shared.generated.resources.map_bheta_decima_1
import killteam.shared.generated.resources.map_bheta_decima_2
import killteam.shared.generated.resources.map_bheta_decima_3
import killteam.shared.generated.resources.map_bheta_decima_4
import killteam.shared.generated.resources.map_bheta_decima_5
import killteam.shared.generated.resources.map_bheta_decima_6
import killteam.shared.generated.resources.map_gallowdark_1
import killteam.shared.generated.resources.map_gallowdark_2
import killteam.shared.generated.resources.map_gallowdark_3
import killteam.shared.generated.resources.map_gallowdark_4
import killteam.shared.generated.resources.map_gallowdark_5
import killteam.shared.generated.resources.map_gallowdark_6
import killteam.shared.generated.resources.map_nonspecific_1
import killteam.shared.generated.resources.map_nonspecific_2
import killteam.shared.generated.resources.map_nonspecific_3
import killteam.shared.generated.resources.map_nonspecific_4
import killteam.shared.generated.resources.map_nonspecific_5
import killteam.shared.generated.resources.map_nonspecific_6
import killteam.shared.generated.resources.map_tomb_world_1
import killteam.shared.generated.resources.map_tomb_world_2
import killteam.shared.generated.resources.map_tomb_world_3
import killteam.shared.generated.resources.map_tomb_world_4
import killteam.shared.generated.resources.map_tomb_world_5
import killteam.shared.generated.resources.map_tomb_world_6
import killteam.shared.generated.resources.map_volkus_1
import killteam.shared.generated.resources.map_volkus_2
import killteam.shared.generated.resources.map_volkus_3
import killteam.shared.generated.resources.map_volkus_4
import killteam.shared.generated.resources.map_volkus_5
import killteam.shared.generated.resources.map_volkus_6
import killteam.shared.generated.resources.recon_flank_example
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

private val mapResources: Map<Killzone, List<DrawableResource>> = mapOf(
    Killzone.NonSpecific to listOf(
        Res.drawable.map_nonspecific_1, Res.drawable.map_nonspecific_2, Res.drawable.map_nonspecific_3,
        Res.drawable.map_nonspecific_4, Res.drawable.map_nonspecific_5, Res.drawable.map_nonspecific_6,
    ),
    Killzone.Volkus to listOf(
        Res.drawable.map_volkus_1, Res.drawable.map_volkus_2, Res.drawable.map_volkus_3,
        Res.drawable.map_volkus_4, Res.drawable.map_volkus_5, Res.drawable.map_volkus_6,
    ),
    Killzone.Gallowdark to listOf(
        Res.drawable.map_gallowdark_1, Res.drawable.map_gallowdark_2, Res.drawable.map_gallowdark_3,
        Res.drawable.map_gallowdark_4, Res.drawable.map_gallowdark_5, Res.drawable.map_gallowdark_6,
    ),
    Killzone.TombWorld to listOf(
        Res.drawable.map_tomb_world_1, Res.drawable.map_tomb_world_2, Res.drawable.map_tomb_world_3,
        Res.drawable.map_tomb_world_4, Res.drawable.map_tomb_world_5, Res.drawable.map_tomb_world_6,
    ),
    Killzone.BhetaDecima to listOf(
        Res.drawable.map_bheta_decima_1, Res.drawable.map_bheta_decima_2, Res.drawable.map_bheta_decima_3,
        Res.drawable.map_bheta_decima_4, Res.drawable.map_bheta_decima_5, Res.drawable.map_bheta_decima_6,
    ),
)

fun mapResource(killzone: Killzone, index: Int): DrawableResource = mapResources.getValue(killzone)[index]

/** Universal map key entries (icon, description) as on Wahapedia. */
val mapKey: List<Pair<DrawableResource, String>> = listOf(
    Res.drawable.key_a_drop to "Player 1 Drop Zone and Territory",
    Res.drawable.key_a_territory to "Player 1 Territory",
    Res.drawable.key_a_edge to "Player 1 Killzone Edge",
    Res.drawable.key_a_obj to "Player 1 Objective Marker",
    Res.drawable.key_b_drop to "Player 2 Drop Zone and Territory",
    Res.drawable.key_b_territory to "Player 2 Territory",
    Res.drawable.key_b_edge to "Player 2 Killzone Edge",
    Res.drawable.key_b_obj to "Player 2 Objective Marker",
    Res.drawable.key_n_obj to "Centre Objective Marker",
    Res.drawable.key_n_edge to "Neutral Killzone Edge",
    Res.drawable.key_centre to "Centre of Killzone",
    Res.drawable.key_centreline to "Centre Line",
)

val killGradeImage: DrawableResource get() = Res.drawable.kill_grade
val reconFlankImage: DrawableResource get() = Res.drawable.recon_flank_example

val selectIcon: DrawableResource get() = Res.drawable.btn_select
val removeIcon: DrawableResource get() = Res.drawable.btn_remove
val effectIcon: DrawableResource get() = Res.drawable.icon_effect
val navContentsIcon: DrawableResource get() = Res.drawable.btn_nav_contents
val navListIcon: DrawableResource get() = Res.drawable.btn_nav_armylist
val navTopIcon: DrawableResource get() = Res.drawable.btn_nav_totop
val conditionIcon: DrawableResource get() = Res.drawable.icon_condition

fun tacOpIcon(archetype: Archetype): DrawableResource = when (archetype) {
    Archetype.Infiltration -> Res.drawable.tacop_infiltration
    Archetype.Recon -> Res.drawable.tacop_recon
    Archetype.Security -> Res.drawable.tacop_security
    Archetype.SeekAndDestroy -> Res.drawable.tacop_destroy
}

/** Pan and zoom surface; the size comes from the modifier. Zoom with pinch, the buttons or drag to pan. */
@Composable
fun ZoomPanel(painter: Painter, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Column(modifier) {
        Box(
            Modifier.weight(1f).fillMaxWidth().clipToBounds().pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 8f)
                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter, null,
                Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                contentScale = ContentScale.Fit,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ scale = (scale * 1.4f).coerceAtMost(8f) }) { Text("Zoom +") }
            OutlinedButton({ scale = (scale / 1.4f).coerceAtLeast(1f); if (scale <= 1f) offset = Offset.Zero }) { Text("Zoom -") }
            OutlinedButton({ scale = 1f; offset = Offset.Zero }) { Text("Reset") }
        }
    }
}

/** Full-screen viewer opened by tapping an image. */
@Composable
fun ImageViewer(painter: Painter, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xF2101010)) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClose) { Text("Close") }
                }
                ZoomPanel(painter, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

/** Image that opens in the full-screen viewer when tapped. */
@Composable
fun ClickableImage(resource: DrawableResource, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.FillWidth) {
    var open by remember { mutableStateOf(false) }
    val painter = painterResource(resource)
    Image(painter, "Tap to enlarge", modifier.clickable { open = true }, contentScale = contentScale)
    if (open) ImageViewer(painter) { open = false }
}

@Composable
fun TokenImage(file: String, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, file) {
        value = runCatching { Res.readBytes("files/img/tokens/$file").decodeToImageBitmap() }.getOrNull()
    }
    var open by remember { mutableStateOf(false) }
    bitmap?.let {
        val painter = remember(it) { BitmapPainter(it) }
        Image(painter, null, modifier.size(size).clickable { open = true }, contentScale = ContentScale.Fit)
        if (open) ImageViewer(painter) { open = false }
    }
}
