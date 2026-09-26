package com.expenseanalyst.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expenseanalyst.core.theme.expenseColors
import com.expenseanalyst.core.util.categoryIconVector

/**
 * A category's badge in the Ledger style: an outline glyph in the text colour on a quiet disc
 * with a hairline ring, and the category's colour reduced to a small dot. Keeping colour to the
 * dot is what lets a screen full of categories stay calm; the old style filled each badge with
 * its saturated seeded colour.
 *
 * The notification's large icon draws the same thing as a bitmap
 * (TransactionAlertNotification.categoryLargeIcon) — keep the two in step.
 */
@Composable
fun CategoryBadge(
    iconName: String,
    colorHex: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val colors = MaterialTheme.expenseColors
    val dot = categoryDotColor(colorHex, MaterialTheme.colorScheme.onSurfaceVariant)
    val dotSize = (size.value * 0.18f).coerceAtLeast(7f).dp
    Box(
        modifier = modifier
            .size(size)
            .background(colors.badge, CircleShape)
            .border(1.dp, colors.hairline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIconVector(iconName),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.5f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-size.value * 0.04f).dp, y = (-size.value * 0.04f).dp)
                .size(dotSize)
                .border(2.dp, colors.badge, CircleShape)
                .background(dot, CircleShape)
        )
    }
}

/**
 * The category's stored colour, softened toward [muted] so seeded saturated hues (#FF7043 and
 * friends) and user-picked ones all sit quietly in the Ledger palette.
 */
fun categoryDotColor(colorHex: String?, muted: Color): Color {
    val parsed = colorHex
        ?.let { if (it.startsWith("#")) it else "#$it" }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: return muted
    return lerp(parsed, muted, 0.35f)
}
