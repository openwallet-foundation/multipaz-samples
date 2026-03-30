package org.multipaz.getstarted.biometrics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun FaceMatchingSection(
    faceExtractor: FaceExtractor,
    faceCaptured: MutableState<FaceEmbedding?>
) {
    var showFaceMatching by remember { mutableStateOf(false) }
    var similarity by remember { mutableStateOf(0f) }

    if (!showFaceMatching) {
        Button(onClick = { showFaceMatching = true }) {
            Text("Face Matching")
        }
    } else {
        Text("Similarity: ${(similarity * 100).roundToInt()}%")

        Camera(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .padding(64.dp),
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

                    if (faceEmbedding != null) {
                        val newSimilarity = faceCaptured.value!!.calculateSimilarity(faceEmbedding)
                        similarity = newSimilarity
                    }
                }
            }
        }

        Button(
            onClick = {
                showFaceMatching = false
                faceCaptured.value = null
            }
        ) {
            Text("Close")
        }
    }
}
