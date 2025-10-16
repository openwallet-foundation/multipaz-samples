package org.multipaz.photoididentityreader

import androidx.compose.runtime.Composable
import kotlinx.io.bytestring.ByteString

@Composable
expect fun rememberImagePicker(
    allowMultiple: Boolean,
    onResult: (fileData: List<ByteString>) -> Unit,
): ImagePicker

expect class ImagePicker(
    allowMultiple: Boolean,
    onLaunch: () -> Unit
) {
    fun launch()
}
