package com.puzzle.photo.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File

class ImageAnalyzer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.55f)
            .build()
    )

    fun analyze(
        context: Context,
        photoPath: String,
        onResult: (detectedLabels: List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(photoPath)))
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    onResult(labels.map { it.text.lowercase() })
                }
                .addOnFailureListener { onError(it) }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun matches(detected: List<String>, acceptable: List<String>): Boolean {
        return detected.any { d ->
            acceptable.any { a ->
                d.contains(a.lowercase()) || a.lowercase().contains(d)
            }
        }
    }
}
