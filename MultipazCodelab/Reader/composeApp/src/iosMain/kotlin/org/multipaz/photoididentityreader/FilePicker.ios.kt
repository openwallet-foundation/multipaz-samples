package org.multipaz.photoididentityreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.io.bytestring.ByteString
import org.multipaz.util.Logger
import org.multipaz.util.toByteArray
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPresentationController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

private const val TAG = "FilePicker"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberFilePicker(
    types: List<String>,
    allowMultiple: Boolean,
    onResult: (fileData: List<ByteString>) -> Unit,
): FilePicker {

    val delegate = object :
      NSObject(),
      UIDocumentPickerDelegateProtocol,
      UIAdaptivePresentationControllerDelegateProtocol {

        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>
        ) {
            val result = mutableListOf<ByteString>()
            for (url in didPickDocumentsAtURLs) {
                url as NSURL
                if (!url.startAccessingSecurityScopedResource()) {
                    throw IllegalStateException("Error accessing file")
                }
                memScoped {
                    val error: ObjCObjectVar<NSError?> = alloc()
                    val data = NSData.dataWithContentsOfURL(url, 0UL, error.ptr)
                    if (data != null) {
                        result.add(ByteString(data.toByteArray()))
                    } else {
                        Logger.e(TAG, "Error opening file at $url: ${error.value}")
                    }
                }
                url.stopAccessingSecurityScopedResource()
            }
            onResult(result)
        }

        override fun documentPickerWasCancelled(
            controller: UIDocumentPickerViewController
        ) {
            onResult(emptyList())
        }

        override fun presentationControllerWillDismiss(
            presentationController: UIPresentationController
        ) {
            (presentationController.presentedViewController as? UIDocumentPickerViewController)
                ?.let { documentPickerWasCancelled(it) }
        }
    }

    return remember {
        FilePicker(
            types = types,
            allowMultiple = allowMultiple,
            onLaunch = {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = types.mapNotNull { mimeType ->
                        if (mimeType == "*/*") {
                            UTTypeItem
                        } else {
                            UTType.typeWithMIMEType(mimeType)
                        }
                    }
                )
                picker.delegate = delegate
                picker.allowsMultipleSelection = allowMultiple
                UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                    viewControllerToPresent = picker,
                    animated = true,
                    completion = {},
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
