package com.example.killteam.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Lets a mouse-type pointer scroll by dragging, with a short fling afterwards.
 * Touch on Windows desktop arrives as mouse input and would otherwise not scroll; real touch and stylus keep their native scrolling.
 */
@Composable
fun Modifier.dragToScroll(state: ScrollableState): Modifier {
    val scope = rememberCoroutineScope()
    return pointerInput(state) {
        var fling: Job? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            fling?.cancel()
            if (down.type != PointerType.Mouse) return@awaitEachGesture
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
            tracker.addPosition(slop.uptimeMillis, slop.position)
            val finished = drag(slop.id) { change ->
                tracker.addPosition(change.uptimeMillis, change.position)
                state.dispatchRawDelta(-change.positionChange().y)
                change.consume()
            }
            if (finished) {
                val velocity = tracker.calculateVelocity().y
                fling = scope.launch {
                    var last = 0f
                    AnimationState(0f, velocity).animateDecay(exponentialDecay()) {
                        val delta = value - last
                        last = value
                        val used = state.dispatchRawDelta(-delta)
                        if (abs(used) < abs(delta) / 2) cancelAnimation()
                    }
                }
            }
        }
    }
}
