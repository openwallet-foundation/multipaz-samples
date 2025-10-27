package org.multipaz.photoididentityreader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.io.bytestring.ByteString
import org.multipaz.context.applicationContext
import org.multipaz.util.Logger

private const val TAG = "FilePicker"

@Composable
actual fun rememberFilePicker(
    types: List<String>,
    allowMultiple: Boolean,
    onResult: (fileData: List<ByteString>) -> Unit,
): FilePicker {
    // TODO: handle allowMultiple = true
    val filePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    val inputStream = applicationContext.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        onResult(listOf(ByteString(bytes)))
                    } else {
                        Logger.e(TAG, "File not found")
                    }
                } else {
                    onResult(emptyList())
                }
            },
        )

    return remember {
        FilePicker(
            types = types,
            allowMultiple = allowMultiple,
            onLaunch = {
                filePicker.launch(
                    types.toTypedArray()
                )
            },
        )
    }
}

actual class FilePicker actual constructor(
    val types: List<String>,
    val allowMultiple: Boolean,
    val onLaunch: () -> Unit
) {
    actual fun launch() {
        onLaunch()
    }
}
