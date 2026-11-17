package com.dinnote.app

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
 * Reusable presentation for structured Islamic notes.
 * Styling is a view concern: never inject formatting tokens into editable text.
 * This component does not change persistence, autosave or editor selection.
 */
enum class IslamicBlockStyle { BODY, QURAN, HADITH, TRANSLATION, ATTENTION, ARABIC }

object IslamicPremiumPalette {
    val Emerald = Color(0xFF075D49)
    val Gold = Color(0xFFC49B47)
    val Ink = Color(0xFF263B37)
    val Cream = Color(0xFFF8F8F3)
}

@Composable
fun IslamicPremiumBlock(
    text: String,
    style: IslamicBlockStyle,
    modifier: Modifier = Modifier
) {
    val arabic = style == IslamicBlockStyle.QURAN ||
        style == IslamicBlockStyle.HADITH ||
        style == IslamicBlockStyle.ARABIC
    val heading = when (style) {
        IslamicBlockStyle.QURAN -> "Firman Allah SWT"
        IslamicBlockStyle.HADITH -> "Sabda Rasulullah ﷺ"
        IslamicBlockStyle.TRANSLATION -> "Maksudnya"
        IslamicBlockStyle.ATTENTION -> "Perhatian"
        else -> null
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (style == IslamicBlockStyle.BODY) Color.White else IslamicPremiumPalette.Cream,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (heading != null) {
                Text(
                    text = heading,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IslamicPremiumPalette.Emerald
                )
            }
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (arabic) TextAlign.Right else TextAlign.Start,
                fontSize = if (arabic) 22.sp else 16.sp,
                lineHeight = if (arabic) 36.sp else 25.sp,
                color = IslamicPremiumPalette.Ink
            )
        }
    }
}
