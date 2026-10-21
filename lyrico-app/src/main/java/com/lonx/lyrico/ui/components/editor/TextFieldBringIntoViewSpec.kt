package com.lonx.lyrico.ui.components.editor

import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * Text fields scroll in their own viewport. Screen chrome insets belong to the parent scroller;
 * inheriting Salt BasicScreen's spec applies those insets again inside the text field.
 */
internal object TextFieldBringIntoViewSpec : BringIntoViewSpec {}
