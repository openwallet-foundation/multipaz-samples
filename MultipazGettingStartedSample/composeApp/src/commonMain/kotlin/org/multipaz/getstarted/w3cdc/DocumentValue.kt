package org.multipaz.getstarted.w3cdc

import androidx.compose.ui.graphics.ImageBitmap

sealed class DocumentValue {
     data class ValueText(
        val title: String,
        val value: String
    ): DocumentValue()

    data class ValueImage(
        val image: ImageBitmap
    ): DocumentValue()
}
