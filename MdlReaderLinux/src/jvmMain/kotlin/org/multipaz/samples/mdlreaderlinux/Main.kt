package org.multipaz.samples.mdlreaderlinux

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var usbVidPid = System.getenv("MULTIPAZ_USB_VID_PID")?.trim().orEmpty()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--usb" -> {
                if (i + 1 < args.size) {
                    usbVidPid = args[i + 1]
                    i += 2
                } else {
                    System.err.println("Error: --usb requires a <vid:pid> argument (e.g. 072f:2200)")
                    exitProcess(2)
                }
            }
            else -> i++
        }
    }

    if (usbVidPid.isEmpty()) {
        System.err.println(
            "Error: USB NFC reader VID:PID not specified.\n" +
                "Run with ./launch-with-hci [--hci <controller>] [--usb <vid:pid>] " +
                "or set MULTIPAZ_USB_VID_PID=<vid:pid>."
        )
        exitProcess(2)
    }

    val parts = usbVidPid.split(":")
    val vendorId = parts.getOrNull(0)?.toIntOrNull(16)
    val productId = parts.getOrNull(1)?.toIntOrNull(16)
    if (parts.size != 2 || vendorId == null || productId == null) {
        System.err.println("Error: Invalid USB VID:PID '$usbVidPid' (expected hex format XXXX:YYYY, e.g. 072f:2200)")
        exitProcess(2)
    }

    val hciDevice = System.getenv("MULTIPAZ_BLE_HCI_DEV")?.takeIf { it.isNotBlank() } ?: "hci0"

    application {
        val windowState = rememberWindowState(
            size = DpSize(640.dp, 720.dp),
            position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Multipaz mDL Reader (Linux)"
        ) {
            MaterialTheme {
                MdlReaderScreen(
                    usbVendorId = vendorId,
                    usbProductId = productId,
                    hciDevice = hciDevice
                )
            }
        }
    }
}
