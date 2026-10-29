package com.example.killteam.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** True on the desktop app, where touch arrives as mouse input and needs bigger targets and a visible scrollbar. */
expect val isDesktop: Boolean

/** Draggable scrollbar for a list; empty on platforms with native touch scrolling. */
@Composable
expect fun ListScrollbar(state: LazyListState, modifier: Modifier)
