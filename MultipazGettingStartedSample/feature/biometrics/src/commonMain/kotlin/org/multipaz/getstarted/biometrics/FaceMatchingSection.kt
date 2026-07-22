package org.multipaz.getstarted.biometrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.multipaz.compose.camera.Camera
import org.multipaz.compose.camera.CameraCaptureResolution
import org.multipaz.compose.camera.CameraFrame
import org.multipaz.compose.camera.CameraSelection
import org.multipaz.facedetection.detectFaces
import org.multipaz.facematch.FaceEmbedding
import org.multipaz.facematch.getFaceEmbeddings
import kotlin.math.roundToInt

/**
 * Full-screen live face-matching flow. This must be hosted outside of any vertically scrollable
 * container, since the [Camera] preview fills the available height.
 */
@Composable
fun FaceMatchingScreen(
    faceExtractor: FaceExtractor,
    faceCaptured: MutableState<FaceEmbedding?>,
    onClose: () -> Unit,
) {
    var similarity by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Camera(
            modifier = Modifier.fillMaxWidth().weight(1f),
            cameraSelection = CameraSelection.DEFAULT_FRONT_CAMERA,
            captureResolution = CameraCaptureResolution.MEDIUM,
            showCameraPreview = true,
        ) { incomingVideoFrame: CameraFrame ->
            val faces = detectFaces(incomingVideoFrame)

            when {
                faces.isNullOrEmpty() -> {
                    similarity = 0f
                }

                faceCaptured.value != null -> {
                    val faceImage = faceExtractor.extractFaceBitmap(
                        incomingVideoFrame,
                        faces[0],
                        faceExtractor.faceMatchModel.imageSquareSize
                    )

                    val faceEmbedding = getFaceEmbeddings(faceImage, faceExtractor.faceMatchModel)

                    if (faceCaptured.value != null && faceEmbedding != null) {
                        val newSimilarity = faceCaptured.value!!.calculateSimilarity(faceEmbedding)
                        similarity = newSimilarity
                    }
                }
            }
        }

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { similarity.coerceIn(0f, 1f) },
        )

        Text(
            text = "Similarity: ${(similarity * 100).roundToInt()}%",
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                faceCaptured.value = null
                onClose()
            }
        ) {
            Text("Close")
        }
    }
}
