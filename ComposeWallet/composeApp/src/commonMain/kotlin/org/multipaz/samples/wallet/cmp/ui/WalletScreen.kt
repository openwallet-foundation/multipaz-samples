package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.multipaz.compose.carousels.DocumentCarousel
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel

@Composable
fun WalletScreen(
    documentModel: DocumentModel,
    onDocumentSelected: (DocumentInfo) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DocumentCarousel(
            modifier = Modifier.fillMaxWidth(),
            documentModel = documentModel,
            onDocumentClicked = { documentInfo ->
                onDocumentSelected(documentInfo)
            },
            emptyDocumentContent = {
                EmptyWalletState()
            }
        )
    }
}


@Composable
private fun EmptyWalletState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val outlineColor = LocalContentColor.current.copy(alpha = 0.35f)

        Box(
            modifier = Modifier
                .offset(y = (-24).dp)
                .fillMaxWidth(0.9f)
                .aspectRatio(1.6f)
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val dashLength = 10.dp.toPx()
                    val gapLength = 6.dp.toPx()

                    drawRoundRect(
                        color = outlineColor,
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(dashLength, gapLength)
                            )
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No documents added yet",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Add a document from issuer.multipaz.org",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}






