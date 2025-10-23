package org.multipaz.photoididentityreader

import androidx.compose.runtime.Composable
import kotlinx.io.bytestring.ByteString

@Composable
expect fun rememberFilePicker(
    types: List<String>,
    allowMultiple: Boolean,
    onResult: (fileData: List<ByteString>) -> Unit,
): FilePicker

expect class FilePicker(
    types: List<String>,
    allowMultiple: Boolean,
    onLaunch: () -> Unit
) {
    fun launch()
}
