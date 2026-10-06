package com.aeromaintenance.ai.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/** Scaffold insets plus the standard 16 dp screen gutter. */
@Composable
fun withScreenPadding(p: PaddingValues): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = p.calculateStartPadding(dir) + 16.dp,
        end = p.calculateEndPadding(dir) + 16.dp,
        top = p.calculateTopPadding() + 8.dp,
        bottom = p.calculateBottomPadding() + 24.dp,
    )
}

/** Scrolls a lazy list to the item the guided demo is pointing at. */
@Composable
fun DemoScroll(state: LazyListState, focus: String?, keys: Map<String, Int>) {
    LaunchedEffect(focus) {
        val index = focus?.let { keys[it] } ?: return@LaunchedEffect
        state.animateScrollToItem(index)
    }
}
