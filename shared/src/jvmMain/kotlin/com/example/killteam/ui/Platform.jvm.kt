package com.example.killteam.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

actual val isDesktop: Boolean = true

@Composable
actual fun ListScrollbar(state: LazyListState, modifier: Modifier) {
    val style = LocalScrollbarStyle.current.copy(
        thickness = 16.dp,
        unhoverColor = Waha.Orange.copy(alpha = 0.55f),
        hoverColor = Waha.Orange,
    )
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier, style = style)
}
