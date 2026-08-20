package chaynik.mizu.util.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.minus

fun PaddingValues.withoutTop() = this.minus(PaddingValues(top = this.calculateTopPadding()))
