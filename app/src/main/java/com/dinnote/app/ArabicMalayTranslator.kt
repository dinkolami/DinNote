package com.dinnote.app

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage

/**
 * ML Kit downloads its language model on first use (internet required).
 * Later translations can run on device using the downloaded model.
 */
object ArabicMalayTranslator {
    fun translate(text: String, onResult: (Result<String>) -> Unit) {
        if (text.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Tiada teks Arab untuk diterjemah.")))
            return
        }
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ARABIC)
                .setTargetLanguage(TranslateLanguage.MALAY)
                .build()
        )
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        onResult(Result.success(translated))
                        translator.close()
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                        translator.close()
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
                translator.close()
            }
    }
}
