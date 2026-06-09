package org.multipaz.getstarted.biometrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.multipaz.compose.decodeImage
import org.multipaz.facematch.FaceEmbedding
import org.multipaz.facematch.getFaceEmbeddings
import org.multipaz.selfiecheck.SelfieCheck
import org.multipaz.selfiecheck.SelfieCheckViewModel

/**
 * Full-screen selfie capture flow. This must be hosted outside of any vertically scrollable
 * container, since [SelfieCheck] fills the available height.
 */
@Composable
fun SelfieCaptureScreen(
    faceExtractor: FaceExtractor,
    identityIssuer: String,
    onFaceCaptured: (FaceEmbedding?) -> Unit,
    onClose: () -> Unit,
) {
    val selfieCheckViewModel: SelfieCheckViewModel =
        remember { SelfieCheckViewModel(identityIssuer) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SelfieCheck(
            modifier = Modifier.wrapContentSize().weight(1f),
            onVerificationComplete = {
                if (selfieCheckViewModel.capturedFaceImage != null) {
                    val embedding = getFaceEmbeddings(
                        image = decodeImage(selfieCheckViewModel.capturedFaceImage!!.toByteArray()),
                        model = faceExtractor.faceMatchModel
                    )
                    onFaceCaptured(embedding)
                }
                selfieCheckViewModel.resetForNewCheck()
                onClose()
            },
            viewModel = selfieCheckViewModel,
            identityIssuer = identityIssuer
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onClick = {
                selfieCheckViewModel.resetForNewCheck()
                onClose()
            }
        ) {
            Text("Close")
        }
    }
}
