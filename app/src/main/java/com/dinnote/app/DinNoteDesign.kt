package com.dinnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared visual components for DinNote 3.0.
 * All note, maktabah and reading screens should use the same border and spacing.
 */
object DinNoteDesign {
    val Turquoise = Color(0xFF075B50)
    val Border = Color(0xFFE3DAC5)
    val Gold = Color(0xFFB48B39)
    val Corner = 14.dp
    val Cream = Color(0xFFFFFDF7)
    val Ink = Color(0xFF183A30)
}

@Composable
fun DinNoteContentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DinNoteDesign.Cream,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(DinNoteDesign.Corner),
        border = BorderStroke(1.dp, DinNoteDesign.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun DinNoteBilingualCard(
    arabic: String,
    malay: String,
    modifier: Modifier = Modifier
) {
    DinNoteContentCard(modifier) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
            text = arabic,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right,
            fontSize = 18.sp,
            lineHeight = 31.sp,
            style = TextStyle(textDirection = TextDirection.Rtl),
            color = Color(0xFF183A30)
        )
                Text(
            text = malay,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = Color(0xFF334B4A)
                )
            }
        }
    }
}

@Composable
fun DinNoteSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = DinNoteDesign.Turquoise
    )
}
