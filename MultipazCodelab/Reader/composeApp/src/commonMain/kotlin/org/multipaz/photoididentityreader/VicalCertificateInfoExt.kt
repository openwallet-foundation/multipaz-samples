package org.multipaz.photoididentityreader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.multipaz.asn1.OID
import org.multipaz.mdoc.vical.VicalCertificateInfo

@Composable
fun VicalCertificateInfo.RenderIconWithFallback(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
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

val VicalCertificateInfo.displayNameWithFallback: String
    get() {
        val subject = certificate.subject
        val commonName = subject.components[OID.COMMON_NAME.oid]
        if (commonName != null) {
            return commonName.value
        }
        return subject.name
    }


