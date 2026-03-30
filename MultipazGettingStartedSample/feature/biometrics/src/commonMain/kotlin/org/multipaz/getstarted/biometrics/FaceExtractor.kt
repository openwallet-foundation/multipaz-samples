package org.multipaz.getstarted.biometrics

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.io.bytestring.ByteString
import multipazgettingstartedsample.feature.biometrics.generated.resources.Res
import org.multipaz.compose.camera.CameraFrame
import org.multipaz.compose.cropRotateScaleImage
import org.multipaz.facedetection.DetectedFace
import org.multipaz.facedetection.FaceLandmarkType
import org.multipaz.facematch.FaceMatchLiteRtModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

class FaceExtractor {

    lateinit var faceMatchModel: FaceMatchLiteRtModel
        private set

    val isInitialized: Boolean get() = ::faceMatchModel.isInitialized

    suspend fun init() {
        val modelData = ByteString(*Res.readBytes("files/facenet_512.tflite"))
        faceMatchModel =
            FaceMatchLiteRtModel(modelData, imageSquareSize = 160, embeddingsArraySize = 512)
    }

    /** Cut out the face square, rotate it to level eyes line, scale to the smaller size for face matching tasks. */
    fun extractFaceBitmap(
        frameData: CameraFrame,
        face: DetectedFace,
        targetSize: Int
    ): ImageBitmap {
        val leftEye = face.landmarks.find { it.type == FaceLandmarkType.LEFT_EYE }
        val rightEye = face.landmarks.find { it.type == FaceLandmarkType.RIGHT_EYE }
        val mouthPosition = face.landmarks.find { it.type == FaceLandmarkType.MOUTH_BOTTOM }

        if (leftEye == null || rightEye == null || mouthPosition == null) {
            return frameData.cameraImage.toImageBitmap()
        }

        val faceCropFactor = 4f
        val faceVerticalOffsetFactor = 0.25f

        var faceCenterX = (leftEye.position.x + rightEye.position.x) / 2
        var faceCenterY = (leftEye.position.y + rightEye.position.y) / 2
        val eyeOffsetX = leftEye.position.x - rightEye.position.x
        val eyeOffsetY = leftEye.position.y - rightEye.position.y
        val eyeDistance = sqrt(eyeOffsetX * eyeOffsetX + eyeOffsetY * eyeOffsetY)
        val faceWidth = eyeDistance * faceCropFactor
        val faceVerticalOffset = eyeDistance * faceVerticalOffsetFactor
        if (frameData.isLandscape) {
            faceCenterY += faceVerticalOffset * (if (leftEye.position.y < mouthPosition.position.y) 1 else -1)
        } else {
            faceCenterX -= faceVerticalOffset * (if (leftEye.position.x < mouthPosition.position.x) -1 else 1)
        }
        val eyesAngleRad = atan2(eyeOffsetY, eyeOffsetX)
        val eyesAngleDeg = eyesAngleRad * 180.0 / PI
        val totalRotationDegrees = 180 - eyesAngleDeg

        return cropRotateScaleImage(
            frameData = frameData,
            cx = faceCenterX.toDouble(),
            cy = faceCenterY.toDouble(),
            angleDegrees = totalRotationDegrees,
            outputWidthPx = faceWidth.toInt(),
            outputHeightPx = faceWidth.toInt(),
            targetWidthPx = targetSize,
        )
    }
}
