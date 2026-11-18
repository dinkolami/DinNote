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
    val Corner = 12.dp
    val Cream = Color(0xFFFFFDF7)
    val Ink = Color(0xFF183A30)
    val SecondaryInk = Color(0xFF334B4A)
    val CardHorizontalPadding = 12.dp
    val CardVerticalPadding = 10.dp
    val ReaderParagraphSpacing = 6.dp
    val ReaderArabicSize = 18.sp
    val ReaderMalaySize = 14.sp
    val ReaderArabicLineHeight = 31.sp
    val ReaderMalayLineHeight = 22.sp
    val SectionTitleSize = 16.sp
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
        shadowElevation = 1.dp,
        shape = RoundedCornerShape(DinNoteDesign.Corner),
        border = BorderStroke(1.dp, DinNoteDesign.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = DinNoteDesign.CardHorizontalPadding, vertical = DinNoteDesign.CardVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(DinNoteDesign.ReaderParagraphSpacing)) {
                if (arabic.isBlank() && malay.isBlank()) {
                    Text(
                        text = "Tiada kandungan untuk dipaparkan.",
                        fontSize = 13.sp,
                        color = DinNoteDesign.SecondaryInk
                    )
                }
                if (arabic.isNotBlank()) {
                    Text(
                        text = arabic,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right,
                        fontSize = DinNoteDesign.ReaderArabicSize,
                        lineHeight = DinNoteDesign.ReaderArabicLineHeight,
                        style = TextStyle(textDirection = TextDirection.Rtl),
                        color = DinNoteDesign.Ink
                    )
                }
                if (malay.isNotBlank()) {
                    Text(
                        text = malay,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = DinNoteDesign.ReaderMalaySize,
                        lineHeight = DinNoteDesign.ReaderMalayLineHeight,
                        textAlign = TextAlign.Start,
                        style = TextStyle(textDirection = TextDirection.Ltr),
                        color = DinNoteDesign.SecondaryInk
                    )
                }
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
