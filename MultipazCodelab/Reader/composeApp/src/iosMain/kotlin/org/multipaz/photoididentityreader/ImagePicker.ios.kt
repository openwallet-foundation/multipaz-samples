package org.multipaz.photoididentityreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.bytestring.ByteString
import org.multipaz.util.Logger
import org.multipaz.util.toByteArray
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume

private const val TAG = "ImagePicker"

@OptIn(BetaInteropApi::class, ExperimentalCoroutinesApi::class)
@Composable
actual fun rememberImagePicker(
    allowMultiple: Boolean,
    onResult: (fileData: List<ByteString>) -> Unit,
): ImagePicker {
    val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(
            picker: PHPickerViewController,
            didFinishPicking: List<*>
        ) {
            picker.dismissViewControllerAnimated(true, null)
            CoroutineScope(Dispatchers.Main).launch {
                @Suppress("UNCHECKED_CAST")
                val results = didFinishPicking as List<PHPickerResult>
                val ret = mutableListOf<ByteString>()
                for (result in results) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        result.itemProvider.loadDataRepresentationForTypeIdentifier(
                            "public.image",
                            completionHandler = { data, error ->
                                if (error == null && data != null) {
                                    ret.add(ByteString(data.toByteArray()))
                                    println("Adding file of size ${data.length}")
                                    continuation.resume(Unit)
                                } else {
                                    Logger.e(TAG, "Error loading data: $error")
                                    continuation.resume(Unit)
                                }
                            }
                        )
                    }
                }
                onResult(ret)
            }
        }
    }

    return remember {
        ImagePicker(
            allowMultiple = allowMultiple,
            onLaunch = {
                val configuration = PHPickerConfiguration()
                configuration.selectionLimit = if (allowMultiple) 0 else 1
                configuration.filter = PHPickerFilter.imagesFilter
                val pickerViewController = PHPickerViewController(configuration)
                pickerViewController.delegate = delegate
                UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                    viewControllerToPresent = pickerViewController,
                    animated = true,
                    completion = {},
                )
            },
        )
    }
}

actual class ImagePicker actual constructor(
    val allowMultiple: Boolean,
    val onLaunch: () -> Unit
) {
    actual fun launch() {
        onLaunch()
    }
}
