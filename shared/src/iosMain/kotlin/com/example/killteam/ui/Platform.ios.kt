package com.example.killteam.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val isDesktop: Boolean = false

@Composable
actual fun ListScrollbar(state: LazyListState, modifier: Modifier) = Unit
