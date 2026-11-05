package com.example.visionwidget.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Sheet state for a sheet that has to ask before it is left with something written in it.
 *
 * The question is raised by refusing the close rather than by answering one that already
 * happened. A sheet can be sent away by the handle, by a drag anywhere in its body, by
 * the scrim and by the back gesture, and those do not all arrive the same way: a drag
 * begun on the body is carried by the scrolling content and reaches the sheet through
 * nested scroll, where whether it ends hidden — and so whether `onDismissRequest` is
 * reached at all — depends on how the settle lands. Every one of them passes through
 * `confirmValueChange` first, so that is where the question belongs. Vetoing also means
 * the sheet is never actually sent away and brought back; it simply stays.
 *
 * [hasUnsavedWork] is read at the moment of the gesture, not when this is called, so it
 * can be a plain expression over the sheet's own fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberGuardedSheetState(
    hasUnsavedWork: () -> Boolean,
    onAsk: () -> Unit
): SheetState {
    val unsaved = rememberUpdatedState(hasUnsavedWork)
    val ask = rememberUpdatedState(onAsk)

    // Held for the life of the sheet: the state below is remembered against this lambda,
    // so handing it a fresh one each pass would rebuild the sheet underneath the drag.
    val confirm = remember {
        { target: SheetValue ->
            if (target == SheetValue.Hidden && unsaved.value()) {
                ask.value()
                false
            } else {
                true
            }
        }
    }

    return rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = confirm
    )
}
