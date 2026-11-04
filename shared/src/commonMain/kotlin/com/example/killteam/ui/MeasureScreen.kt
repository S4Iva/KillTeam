package com.example.killteam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.killteam.logic.AppState
import com.example.killteam.logic.LengthUnit
import com.example.killteam.logic.Measure
import com.example.killteam.logic.MeasurePresets
import com.example.killteam.logic.MeasureSettings
import com.example.killteam.logic.Pt
import com.example.killteam.logic.RuleDistance
import com.example.killteam.logic.Scale

private enum class CalMethod(val label: String, val needed: Int) {
    Length("Known length (2 points)", 2),
    Plane("Table plane (4 corners)", 4),
}

private const val MAX_ZOOM = 8f

private fun Offset.pt() = Pt(x.toDouble(), y.toDouble())

/** View state of the measuring screen. Points live in picture coordinates; zoom and pan only change how they are shown. */
private class MeasureState {
    var frozen by mutableStateOf(false)
    var zoom by mutableStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
    var viewSize by mutableStateOf(IntSize.Zero)

    var calibrating by mutableStateOf(true)
    var method by mutableStateOf(CalMethod.Length)
    val calPoints = mutableStateListOf<Offset>()
    var knownMm by mutableStateOf("40")
    var planeWidth by mutableStateOf("762")
    var planeDepth by mutableStateOf("558.8")
    var scale: Scale? by mutableStateOf(null)
    var warning: String? by mutableStateOf(null)

    val points = mutableStateListOf<Offset>()
    val selected = mutableStateListOf<Int>()
    var between by mutableStateOf(false)

    val items get() = if (calibrating) calPoints else points

    private val center get() = Offset(viewSize.width / 2f, viewSize.height / 2f)

    fun toScreen(p: Offset): Offset = center + (p - center) * zoom + pan

    fun toContent(s: Offset): Offset = (s - pan - center) / zoom + center

    private fun clampPan() {
        val maxX = (zoom - 1f) * viewSize.width / 2f
        val maxY = (zoom - 1f) * viewSize.height / 2f
        pan = Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
    }

    /** Zooms by [factor] keeping the screen point [focus] fixed, then moves the picture by [move]. */
    fun zoomBy(factor: Float, focus: Offset, move: Offset = Offset.Zero) {
        val next = (zoom * factor).coerceIn(1f, MAX_ZOOM)
        pan = focus - center - (focus - pan - center) * (next / zoom) + move
        zoom = next
        clampPan()
    }

    fun panBy(delta: Offset) {
        pan += delta
        clampPan()
    }

    fun resetZoom() {
        zoom = 1f
        pan = Offset.Zero
    }

    fun nearest(screen: Offset, radius: Float): Int =
        items.indices.minByOrNull { (toScreen(items[it]) - screen).getDistance() }
            ?.takeIf { (toScreen(items[it]) - screen).getDistance() <= radius } ?: -1

    fun move(index: Int, to: Offset) {
        items[index] = Offset(to.x.coerceIn(0f, viewSize.width.toFloat()), to.y.coerceIn(0f, viewSize.height.toFloat()))
    }

    fun tap(screen: Offset, index: Int) {
        if (index >= 0) {
            if (!calibrating) toggleSelected(index)
        } else if (!calibrating) {
            points.add(toContent(screen))
        } else if (calPoints.size < method.needed) {
            calPoints.add(toContent(screen))
        }
    }

    private fun toggleSelected(index: Int) {
        if (index in selected) selected.remove(index)
        else {
            if (!between) selected.clear()
            else if (selected.size >= 2) selected.removeAt(0)
            selected.add(index)
        }
    }

    fun deleteSelected() {
        selected.sortedDescending().forEach { points.removeAt(it) }
        selected.clear()
    }

    fun undo() {
        if (points.isEmpty()) return
        points.removeAt(points.lastIndex)
        selected.removeAll { it >= points.size }
    }

    fun clearPoints() {
        points.clear()
        selected.clear()
    }

    fun distanceMm(a: Offset, b: Offset): Double? = scale?.distanceMm(a.pt(), b.pt())

    /** Applies the calibration; returns an error text or null on success. */
    fun applyCalibration(): String? {
        val new: Scale = when (method) {
            CalMethod.Length -> {
                val mm = Measure.parseNumber(knownMm) ?: return "Enter the known length in mm."
                Measure.linearScale(calPoints[0].pt(), calPoints[1].pt(), mm) ?: return "The two points are too close together or the length is not positive."
            }
            CalMethod.Plane -> {
                val w = Measure.parseNumber(planeWidth)
                val d = Measure.parseNumber(planeDepth)
                if (w == null || d == null) return "Enter width and depth in mm."
                Measure.planeScale(calPoints.map { it.pt() }, w, d) ?: return "The corners do not form a valid rectangle. Check the order and the sizes."
            }
        }
        warning = when (method) {
            CalMethod.Length ->
                if (Measure.calibrationTooShort(calPoints[0].pt(), calPoints[1].pt(), viewSize.width.toDouble()))
                    "The reference is very short in the picture. Use a longer reference for better accuracy." else null
            CalMethod.Plane ->
                if (Measure.obliqueRatio(calPoints.map { it.pt() }) > 1.8)
                    "The shot is strongly angled. Hold the camera more directly above the table." else null
        }
        scale = new
        calibrating = false
        selected.clear()
        return null
    }
}

@Composable
fun MeasureScreen(app: AppState, onBack: () -> Unit) {
    val source = rememberCameraSource()
    if (source == null) {
        Page("Measure distances", onBack) { item { Text("Measuring is not available on this device.") } }
        return
    }
    val st = remember { MeasureState() }
    val settings = app.measure.value
    val update = { s: MeasureSettings -> app.measure.update(s) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelMax = maxHeight * 0.42f
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Palette.Header).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                AppButton("< Back", onBack, kind = ButtonKind.Text)
                Text("Measure distances", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(start = 4.dp))
            }
            HorizontalDivider(thickness = 2.dp, color = Palette.Accent.copy(alpha = 0.6f))
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color.Black)) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    scaleX = st.zoom
                    scaleY = st.zoom
                    translationX = st.pan.x
                    translationY = st.pan.y
                }) {
                    if (source.message == null) source.Content(Modifier.fillMaxSize(), st.frozen)
                }
                val message = source.message
                if (message != null) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message, color = Color.White, textAlign = TextAlign.Center)
                        source.actionLabel?.let { AppButton(it, source::action, Modifier.padding(top = 12.dp)) }
                    }
                } else {
                    MeasureOverlay(st, settings)
                    ViewToolbar(st, source)
                }
            }
            HorizontalDivider(thickness = 2.dp, color = Palette.Accent.copy(alpha = 0.6f))
            Column(
                Modifier.fillMaxWidth().heightIn(max = panelMax).background(Palette.Card).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (st.calibrating) CalibrationPanel(st) else MeasurePanel(st, settings, update)
            }
        }
    }

    if (!settings.noticeSeen) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { AppButton("OK", { update(settings.copy(noticeSeen = true)) }) },
            title = { Text("Accuracy") },
            text = {
                Text(
                    "Camera measuring is an aid. With careful calibration and the camera held straight above the table the error is typically 2-3 mm. " +
                        "In a dispute the tape measure decides. The picture is processed on this device only and is never saved.",
                )
            },
        )
    }
}

@Composable
private fun ViewToolbar(st: MeasureState, source: CameraSource) {
    val colors = ButtonDefaults.buttonColors(containerColor = Palette.Header.copy(alpha = 0.85f), contentColor = Color.White)
    val pad = PaddingValues(horizontal = 12.dp)
    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val other = source.toolbarLabel
            if (other != null) {
                Button(source::toolbarAction, Modifier.heightIn(min = 48.dp), colors = colors, contentPadding = pad) { Text(other) }
            } else {
                Button({ st.frozen = !st.frozen }, Modifier.heightIn(min = 48.dp), colors = colors, contentPadding = pad) { Text(if (st.frozen) "Live" else "Freeze") }
            }
            val mid = Offset(st.viewSize.width / 2f, st.viewSize.height / 2f)
            Button({ st.zoomBy(1.5f, mid) }, Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp), colors = colors, contentPadding = pad) { Text("+") }
            Button({ st.zoomBy(1f / 1.5f, mid) }, Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp), colors = colors, contentPadding = pad) { Text("-") }
            Button(st::resetZoom, Modifier.heightIn(min = 48.dp), colors = colors, contentPadding = pad) { Text("1x") }
        }
    }
}

@Composable
private fun MeasureOverlay(st: MeasureState, settings: MeasureSettings) {
    val measurer = rememberTextMeasurer()
    Canvas(
        Modifier.fillMaxSize()
            .onSizeChanged { st.viewSize = it }
            .pointerInput(Unit) { measureGestures(st) },
    ) {
        val unit = settings.unit
        val rule = settings.rule
        val radius = 9.dp.toPx()
        val stroke = Stroke(2.dp.toPx())
        val screen = st.items.map { st.toScreen(it) }

        fun ruleColor(mm: Double?) = when {
            mm == null || rule == null -> Palette.Accent
            rule.contains(mm) -> Palette.Effect
            else -> Palette.Condition
        }

        fun segment(i: Int, j: Int, color: Color, label: String?) {
            drawLine(color, screen[i], screen[j], strokeWidth = 3.dp.toPx())
            if (label != null) drawLabel(measurer, label, (screen[i] + screen[j]) / 2f, color)
        }

        if (st.calibrating) {
            val color = Palette.Highlight
            if (st.method == CalMethod.Length && screen.size == 2) segment(0, 1, color, null)
            if (st.method == CalMethod.Plane && screen.size > 1) {
                for (i in 0 until screen.size - 1) segment(i, i + 1, color, null)
                if (screen.size == 4) segment(3, 0, color, null)
            }
            screen.forEachIndexed { i, p ->
                drawMarker(p, color, radius, stroke)
                drawLabel(measurer, if (st.method == CalMethod.Length) "AB"[i].toString() else (i + 1).toString(), p + Offset(0f, -(radius + 16.dp.toPx())), Color.Black)
            }
        } else {
            if (!st.between) {
                for (i in 0 until screen.size - 1) {
                    val mm = st.distanceMm(st.points[i], st.points[i + 1])
                    segment(i, i + 1, ruleColor(mm), mm?.let { Measure.format(it, unit) })
                }
            } else if (st.selected.size == 2) {
                val a = st.selected[0]
                val b = st.selected[1]
                val mm = st.distanceMm(st.points[a], st.points[b])
                val color = ruleColor(mm)
                drawLine(color, screen[a], screen[b], strokeWidth = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
                drawLabel(measurer, mm?.let { Measure.format(it, unit) } ?: "?", (screen[a] + screen[b]) / 2f, color)
            }
            screen.forEachIndexed { i, p ->
                val chosen = i in st.selected
                drawMarker(p, if (chosen) Palette.Highlight else Color.White, if (chosen) radius * 1.4f else radius, stroke)
                drawLabel(measurer, (i + 1).toString(), p + Offset(0f, -(radius + 16.dp.toPx())), Color.Black)
            }
        }
    }
}

private fun DrawScope.drawMarker(p: Offset, color: Color, radius: Float, stroke: Stroke) {
    drawCircle(Color.Black.copy(alpha = 0.55f), radius + 2.dp.toPx(), p)
    drawCircle(color, radius, p, style = stroke)
    val arm = radius + 8.dp.toPx()
    drawLine(color, p - Offset(arm, 0f), p + Offset(arm, 0f), strokeWidth = 1.dp.toPx())
    drawLine(color, p - Offset(0f, arm), p + Offset(0f, arm), strokeWidth = 1.dp.toPx())
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, center: Offset, background: Color) {
    val layout = measurer.measure(text, TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    val pad = 4.dp.toPx()
    val topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
    drawRoundRect(
        background.copy(alpha = 0.85f),
        topLeft - Offset(pad, pad / 2f),
        Size(layout.size.width + 2 * pad, layout.size.height + pad),
        CornerRadius(6.dp.toPx()),
    )
    drawText(layout, topLeft = topLeft)
}

/** One finger sets, selects and drags points (or pans a zoomed picture); two fingers zoom and pan. */
private suspend fun PointerInputScope.measureGestures(st: MeasureState) {
    val hitRadius = 28.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = down.position
        var dragged = st.nearest(start, hitRadius)
        var moved = false
        var multi = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                multi = true
                dragged = -1
                st.zoomBy(event.calculateZoom(), event.calculateCentroid(), event.calculatePan())
                event.changes.forEach { it.consume() }
            } else if (!multi) {
                val change = event.changes[0]
                if (!moved && (change.position - start).getDistance() > viewConfiguration.touchSlop) moved = true
                if (moved) {
                    if (dragged >= 0) st.move(dragged, st.toContent(change.position)) else st.panBy(change.positionChange())
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
        if (!moved && !multi) st.tap(start, dragged)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalibrationPanel(st: MeasureState) {
    var error by remember { mutableStateOf<String?>(null) }
    Text("1. Calibrate  2. Set points  3. Read the distance", fontWeight = FontWeight.Bold)
    Text("Hold the camera straight above the table. The distances are valid for the table plane only.", style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalMethod.entries.forEach { m ->
            FilterChip(st.method == m, { st.method = m; st.calPoints.clear(); error = null }, { Text(m.label) })
        }
    }
    when (st.method) {
        CalMethod.Length -> {
            Text("Tap two points a known distance apart (both sides of a base, a ruler), then enter the length.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurePresets.references.forEach { p ->
                    FilterChip(Measure.parseNumber(st.knownMm) == p.mm, { st.knownMm = Measure.fixed(p.mm, 1).removeSuffix(".0") }, { Text(p.label) })
                }
            }
            OutlinedTextField(st.knownMm, { st.knownMm = it }, Modifier.fillMaxWidth(), label = { Text("Length between the points (mm)") }, singleLine = true)
        }
        CalMethod.Plane -> {
            Text("Tap the corners of a rectangle of known size: 1 top-left, 2 top-right, 3 bottom-right, 4 bottom-left.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeasurePresets.planes.forEach { p ->
                    FilterChip(
                        Measure.parseNumber(st.planeWidth) == p.widthMm && Measure.parseNumber(st.planeDepth) == p.heightMm,
                        { st.planeWidth = Measure.fixed(p.widthMm, 1).removeSuffix(".0"); st.planeDepth = Measure.fixed(p.heightMm, 1).removeSuffix(".0") },
                        { Text(p.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(st.planeWidth, { st.planeWidth = it }, Modifier.weight(1f), label = { Text("Width 1-2 (mm)") }, singleLine = true)
                OutlinedTextField(st.planeDepth, { st.planeDepth = it }, Modifier.weight(1f), label = { Text("Depth 2-3 (mm)") }, singleLine = true)
            }
        }
    }
    Text("Points set: ${st.calPoints.size} of ${st.method.needed}. Drag a point to adjust it.")
    error?.let { Text(it, color = Palette.Condition) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton("Apply", { error = st.applyCalibration() }, enabled = st.calPoints.size == st.method.needed)
        AppButton("Clear points", { st.calPoints.clear(); error = null }, kind = ButtonKind.Secondary, enabled = st.calPoints.isNotEmpty())
        if (st.scale != null) AppButton("Cancel", { st.calibrating = false }, kind = ButtonKind.Text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasurePanel(st: MeasureState, settings: MeasureSettings, update: (MeasureSettings) -> Unit) {
    val scale = st.scale ?: return
    val unit = settings.unit
    val rule = settings.rule
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!st.between, { st.between = false; st.selected.clear() }, { Text("Path") })
        FilterChip(st.between, { st.between = true; st.selected.clear() }, { Text("Between points") })
        LengthUnit.entries.forEach { u -> FilterChip(unit == u, { update(settings.copy(unit = u)) }, { Text(u.label) }) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(rule == null, { update(settings.copy(rule = null)) }, { Text("No template") })
        RuleDistance.entries.forEach { r -> FilterChip(rule == r, { update(settings.copy(rule = r)) }, { Text(r.label) }) }
    }

    fun ruleNote(mm: Double): String = when {
        rule == null -> ""
        rule.contains(mm) -> "  within ${rule.label}"
        else -> "  beyond ${rule.label}"
    }

    if (!st.between) {
        if (st.points.size < 2) Text("Tap in the picture to set points; at least two give a distance. Tap a point to select it, drag it to move it.")
        else {
            val total = Measure.pathMm(st.points.map { it.pt() }, scale)
            Text("Total: ${Measure.format(total, unit)} over ${st.points.size} points", fontWeight = FontWeight.Bold)
            for (i in 0 until st.points.size - 1) {
                val mm = st.distanceMm(st.points[i], st.points[i + 1]) ?: continue
                Text("${i + 1} to ${i + 2}: ${Measure.format(mm, unit)}${ruleNote(mm)}")
            }
        }
    } else {
        if (st.points.size < 2) Text("Tap in the picture to set points, then tap two of them to measure between them.")
        else if (st.selected.size < 2) Text("Tap two points to measure between them (${st.selected.size} of 2 selected).")
        else {
            val a = st.selected[0]
            val b = st.selected[1]
            val mm = st.distanceMm(st.points[a], st.points[b])
            if (mm != null) Text("Point ${a + 1} to ${b + 1}: ${Measure.format(mm, unit)}${ruleNote(mm)}", fontWeight = FontWeight.Bold)
        }
    }
    st.warning?.let { Text(it, color = Palette.Highlight, style = MaterialTheme.typography.bodySmall) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton("Delete selected", st::deleteSelected, kind = ButtonKind.Danger, size = ButtonSize.S, enabled = st.selected.isNotEmpty())
        AppButton("Undo", st::undo, kind = ButtonKind.Secondary, size = ButtonSize.S, enabled = st.points.isNotEmpty())
        AppButton("Clear all", st::clearPoints, kind = ButtonKind.Danger, size = ButtonSize.S, enabled = st.points.isNotEmpty())
        AppButton("Recalibrate", { st.calibrating = true }, kind = ButtonKind.Secondary, size = ButtonSize.S)
    }
}
