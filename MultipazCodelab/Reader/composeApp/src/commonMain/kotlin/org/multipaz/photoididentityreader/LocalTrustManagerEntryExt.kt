package org.multipaz.photoididentityreader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.multipaz.asn1.OID
import org.multipaz.compose.decodeImage
import org.multipaz.mdoc.vical.SignedVical
import org.multipaz.trustmanagement.TrustEntry
import org.multipaz.trustmanagement.TrustEntryVical
import org.multipaz.trustmanagement.TrustEntryX509Cert

val TrustEntry.displayNameWithFallback: String
    get() {
        when (this) {
            is TrustEntryX509Cert -> {
                metadata.displayName?.let { return it }
                val subject = certificate.subject
                val commonName = subject.components[OID.COMMON_NAME.oid]
                if (commonName != null) {
                    return commonName.value
                }
                return subject.name
            }
            is TrustEntryVical -> {
                metadata.displayName?.let { return it }
                // TODO: fish out Vical name
                return "VICAL"
            }
        }
    }

val TrustEntry.displayTypeName: String
    get() {
        when (this) {
            is TrustEntryX509Cert -> {
                return "IACA certificate"
            }
            is TrustEntryVical -> {
                // TODO: if this is slow we can use memoization to speed it up
                val signedVical = SignedVical.parse(
                    encodedSignedVical.toByteArray(),
                    disableSignatureVerification = true
                )
                return "VICAL containing ${signedVical.vical.certificateInfos.size} IACA certificates"
            }
        }
    }

@Composable
fun TrustEntry.RenderIconWithFallback(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    forceFallback: Boolean = false
) {
    if (metadata.displayIcon != null && !forceFallback) {
        val imageBitmap = remember { decodeImage(metadata.displayIcon!!.toByteArray()) }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
            )
        }
        return
    }

    val name = displayNameWithFallback
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")

    val color = Color(name.hashCode().toLong() or 0xFF000000) // Ensure alpha is not zero

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = TextStyle(color = Color.White, fontSize = (size.value/2).sp)
        )
    }
}
