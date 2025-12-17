package org.multipaz.getstarted

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.DataItem
import org.multipaz.compose.decodeImage
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.getstarted.w3cdc.DocumentValue
import org.multipaz.getstarted.w3cdc.TAG
import org.multipaz.util.Logger
import org.multipaz.verification.MdocVerifiedPresentation
import org.multipaz.verification.VerificationUtil.verifyOpenID4VPResponse
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun ShowResponseScreen(
    vpToken: JsonObject?,
    sessionTranscript: DataItem?,
    nonce: ByteString?,
    documentTypeRepository: DocumentTypeRepository?,
    goBack: () -> Unit
) {

    val verificationResult =
        remember { mutableStateOf<VerificationResult>(VerificationResult.Loading) }
    val verificationResultValue = verificationResult.value

    LaunchedEffect(Unit) {
        val now = Clock.System.now()
        if (sessionTranscript == null) {
            verificationResult.value = VerificationResult.Error("Session transcript is null")
            return@LaunchedEffect
        }
        try {
            verificationResult.value = parseResponse(
                now = now,
                vpToken = vpToken,
                sessionTranscript = sessionTranscript,
                nonce = nonce,
                documentTypeRepository = documentTypeRepository,
            )
        } catch (e: Throwable) {
            Logger.e(TAG, "Error parsing response", e)
            verificationResult.value = VerificationResult.Error("Error parsing response")
        }
    }

    when (verificationResultValue) {
        is VerificationResult.Error -> Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Error: ${verificationResultValue.errorMessage}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        is VerificationResult.Loading -> Box(
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is VerificationResult.Success -> SuccessScreen(
            values = verificationResultValue.documentValues,
            goBack = goBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessScreen(
    values: List<DocumentValue>,
    goBack: () -> Unit
) {
    val photo = values.firstOrNull { it is DocumentValue.ValueImage } as? DocumentValue.ValueImage
    val documentNumber = values.firstOrNull {
        it is DocumentValue.ValueText && it.title == "License Number"
    } as? DocumentValue.ValueText

    val documentValues = values
        .filter { it is DocumentValue.ValueText }
        .map { it as DocumentValue.ValueText }
        .filter { it.title != "License Number" && it.title != "EDL Indicator" }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Digital Credential",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W700
                    )
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(vertical = 8.dp, horizontal = 16.dp)

                        ) {
                            Text(
                                modifier = Modifier,
                                text = "Driving License",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        documentNumber?.let {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = it.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )

                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = it.value,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.W700
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        photo?.let {
                            Image(
                                modifier = Modifier
                                    .size(150.dp)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                bitmap = it.image,
                                contentScale = ContentScale.Crop,
                                contentDescription = "Profile photo"
                            )
                        }

                        LazyColumn {
                            items(documentValues) { value ->
                                Column {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        text = value.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    Text(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        text = value.value
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalTime::class)
private suspend fun parseResponse(
    now: Instant,
    vpToken: JsonObject?,
    sessionTranscript: DataItem,
    nonce: ByteString?,
    documentTypeRepository: DocumentTypeRepository?
): VerificationResult {
    val documentValues: MutableList<DocumentValue> = mutableListOf()

    val verifiedPresentations = if (vpToken != null) {
        verifyOpenID4VPResponse(
            now = now,
            vpToken = vpToken,
            sessionTranscript = sessionTranscript,
            nonce = nonce!!,
            documentTypeRepository = documentTypeRepository,
            zkSystemRepository = null
        )
    } else {
        throw IllegalStateException("vpToken must be non-null")
    }

    verifiedPresentations.forEachIndexed { vpNum, verifiedPresentation ->
        if (verifiedPresentation is MdocVerifiedPresentation) {
            verifiedPresentation.issuerSignedClaims.forEach { claim ->
                if (claim.attribute?.type == DocumentAttributeType.Picture) {
                    val image = decodeImage(claim.value.asBstr)
                    documentValues.add(
                        DocumentValue.ValueImage(
                            image
                        )
                    )
                } else {
                    documentValues.add(
                        DocumentValue.ValueText(
                            title = claim.attribute?.displayName ?: "Unknown",
                            value = claim.render()
                        )
                    )
                }
            }
        }
    }
    return VerificationResult.Success(documentValues)
}

private sealed class VerificationResult() {
    data object Loading : VerificationResult()
    data class Success(val documentValues: List<DocumentValue>) : VerificationResult()
    data class Error(val errorMessage: String) : VerificationResult()
}