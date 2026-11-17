package org.multipaz.samples.isomdocreaderlinux

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.multipaz.util.Logger
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Logger.isDebugEnabled = true
    var usbVidPid = System.getenv("MULTIPAZ_USB_VID_PID")?.trim().orEmpty()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--usb" -> {
                if (i + 1 < args.size) {
                    usbVidPid = args[i + 1].trim()
                    i += 2
                } else {
                    System.err.println("Error: --usb requires a <vid:pid> argument (e.g. 072f:2200)")
                    exitProcess(2)
                }
            }
            else -> i++
        }
    }

    var vendorId: Int? = null
    var productId: Int? = null
    if (usbVidPid.isNotEmpty()) {
        val parts = usbVidPid.split(":")
        val parsedVid = parts.getOrNull(0)?.toIntOrNull(16)
        val parsedPid = parts.getOrNull(1)?.toIntOrNull(16)
        if (parts.size != 2 || parsedVid == null || parsedPid == null) {
            System.err.println("Error: Invalid USB VID:PID '$usbVidPid' (expected hex format XXXX:YYYY, e.g. 072f:2200)")
            exitProcess(2)
        }
        vendorId = parsedVid
        productId = parsedPid
    }

    val hciDevice = System.getenv("MULTIPAZ_BLE_HCI_DEV")?.takeIf { it.isNotBlank() } ?: "hci0"

    application {
        val windowState = rememberWindowState(
            size = DpSize(640.dp, 760.dp),
            position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Multipaz mdoc reader (Linux)"
        ) {
            MaterialTheme {
                IsoMdocReaderScreen(
                    usbVendorId = vendorId,
                    usbProductId = productId,
                    hciDevice = hciDevice
                )
            }
        }
    }
}
