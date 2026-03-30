package org.multipaz.getstarted.biometrics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.multipaz.compose.decodeImage
import org.multipaz.facematch.FaceEmbedding
import org.multipaz.facematch.getFaceEmbeddings
import org.multipaz.selfiecheck.SelfieCheck
import org.multipaz.selfiecheck.SelfieCheckViewModel

@Composable
fun SelfieCheckSection(
    faceExtractor: FaceExtractor,
    identityIssuer: String,
    onFaceCaptured: (FaceEmbedding?) -> Unit
) {
    var showCamera by remember { mutableStateOf(false) }
    val selfieCheckViewModel: SelfieCheckViewModel =
        remember { SelfieCheckViewModel(identityIssuer) }

    if (!showCamera) {
        Button(onClick = { showCamera = true }) {
            Text("Selfie Check")
        }
    } else {
        SelfieCheck(
            modifier = Modifier.fillMaxWidth(),
            onVerificationComplete = {
                showCamera = false
                if (selfieCheckViewModel.capturedFaceImage != null) {
                    val embedding = getFaceEmbeddings(
                        image = decodeImage(selfieCheckViewModel.capturedFaceImage!!.toByteArray()),
                        model = faceExtractor.faceMatchModel
                    )
                    onFaceCaptured(embedding)
                }
                selfieCheckViewModel.resetForNewCheck()
            },
            viewModel = selfieCheckViewModel,
            identityIssuer = identityIssuer
        )

        Button(
            onClick = {
                showCamera = false
                selfieCheckViewModel.resetForNewCheck()
            }
        ) {
            Text("Close")
        }
    }
}
