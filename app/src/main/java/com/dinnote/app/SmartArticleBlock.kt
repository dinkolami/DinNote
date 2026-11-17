package com.dinnote.app

/**
 * Structured article sections for the DinNote Smart Block editor.
 * Pure Kotlin model: no Android dependencies and no mutation of existing notes.
 */
enum class SmartBlockKind {
    PARAGRAPH, QURAN, TRANSLATION, HADITH, KITAB, SHARAH
}

data class SmartArticleBlock(
    val id: String,
    val kind: SmartBlockKind,
    val text: String,
    val source: String = "",
    val reference: String = ""
)

/** Insert a block after the selected section, preserving all existing sections. */
fun insertSmartBlock(
    blocks: List<SmartArticleBlock>,
    afterId: String?,
    block: SmartArticleBlock
): List<SmartArticleBlock> {
    require(blocks.none { it.id == block.id }) { "Duplicate block id" }
    val index = blocks.indexOfFirst { it.id == afterId }
    val insertAt = if (index < 0) blocks.size else index + 1
    return blocks.toMutableList().apply { add(insertAt, block) }
}

/** Update only the requested block; never apply formatting to the entire article. */
fun updateSmartBlockText(
    blocks: List<SmartArticleBlock>,
    id: String,
    newText: String
): List<SmartArticleBlock> =
    blocks.map { if (it.id == id) it.copy(text = newText) else it }

/** Determine reading direction from the first strong Arabic or Latin character. */
fun isArabicFirst(text: String): Boolean {
    for (char in text) {
        if (char in '\u0600'..'\u06FF' ||
            char in '\u0750'..'\u077F' ||
            char in '\u08A0'..'\u08FF') return true
        if (char in 'A'..'Z' || char in 'a'..'z') return false
    }
    return false
}
