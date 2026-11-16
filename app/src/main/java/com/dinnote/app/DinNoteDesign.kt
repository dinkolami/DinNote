package com.dinnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared visual components for DinNote 3.0.
 * All note, maktabah and reading screens should use the same border and spacing.
 */
object DinNoteDesign {
    val Turquoise = Color(0xFF087F8C)
    val Border = Color(0xFFD8DEE3)
    val Gold = Color(0xFFC69A3C)
    val Corner = 9.dp
}

@Composable
fun DinNoteContentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
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
        Text(
            text = arabic,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right,
            fontSize = 21.sp,
            lineHeight = 35.sp,
            color = Color(0xFF183A30)
        )
        HorizontalDivider(color = DinNoteDesign.Border, thickness = 1.dp)
        Text(
            text = malay,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = Color(0xFF334B4A)
        )
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
