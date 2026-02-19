package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.back
import mpzcmpwallet.composeapp.generated.resources.cert_expected_update
import mpzcmpwallet.composeapp.generated.resources.cert_signed_date
import mpzcmpwallet.composeapp.generated.resources.cert_valid_from
import mpzcmpwallet.composeapp.generated.resources.cert_valid_until
import mpzcmpwallet.composeapp.generated.resources.certificate_info
import mpzcmpwallet.composeapp.generated.resources.collapse
import mpzcmpwallet.composeapp.generated.resources.expand
import mpzcmpwallet.composeapp.generated.resources.id_usage_description
import mpzcmpwallet.composeapp.generated.resources.learn_more
import mpzcmpwallet.composeapp.generated.resources.type_info
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mpzcmpwallet.composeapp.generated.resources.not_set
import kotlin.time.Instant
import org.multipaz.cbor.Bstr
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.claim.MdocClaim
import org.multipaz.compose.claim.RenderClaimValue
import org.multipaz.compose.datetime.formattedDate
import org.multipaz.compose.document.DocumentModel
import org.multipaz.documenttype.DocumentAttribute
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.sdjwt.SdJwt
import org.multipaz.sdjwt.SdJwt.Companion
import org.multipaz.sdjwt.credential.SdJwtVcCredential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentClaimsScreen(
    documentId: String,
    documentModel: DocumentModel,
    documentTypeRepository: DocumentTypeRepository,
    onBack: () -> Unit,
) {
    val documentInfo = documentModel.documentInfos.collectAsState().value.find {
        it.document.identifier == documentId
    }
    var claims by remember { mutableStateOf<List<Claim>>(emptyList()) }
    var certSignedDate by remember { mutableStateOf<Instant?>(null) }
    var certValidFrom by remember { mutableStateOf<Instant?>(null) }
    var certValidUntil by remember { mutableStateOf<Instant?>(null) }
    var certExpectedUpdate by remember { mutableStateOf<Instant?>(null) }
    var certInfoExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        val document = documentInfo?.document ?: return@LaunchedEffect
        val credentials = document.getCertifiedCredentials()
        val credential = credentials.firstOrNull() ?: return@LaunchedEffect
        claims = credential.getClaims(documentTypeRepository)
        if (credential is MdocCredential) {
            val mso = credential.mso
            certSignedDate = mso.signedAt
            certValidFrom = mso.validFrom
            certValidUntil = mso.validUntil
            certExpectedUpdate = mso.expectedUpdate
        } else if (credential is SdJwtVcCredential) {
            val sdJwt = SdJwt.fromCompactSerialization(credential.issuerProvidedData.decodeToString())
            certSignedDate = sdJwt.issuedAt
            certValidFrom = sdJwt.validFrom
            certValidUntil = sdJwt.validUntil
        }
    }

    val typeDisplayName = documentInfo?.document?.typeDisplayName.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            documentInfo?.let { info ->
                Image(
                    bitmap = info.cardArt,
                    contentDescription = typeDisplayName,
                    modifier = Modifier
                        .size(width = 100.dp, height = 63.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.type_info, typeDisplayName),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(Res.string.id_usage_description))
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(stringResource(Res.string.learn_more))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: Need better renderers for complex claims like Driving Privileges
            //   for mDL and object claims in JSON based credentials. This should probably
            //   be done in the multipaz core library

            val jsonIgnoredClaims = setOf("iss", "vct", "iat", "nbf", "exp", "cnf", "status")
            claims.forEach { claim ->
                if (claim is JsonClaim) {
                    val name = claim.claimPath[0].jsonPrimitive.content
                    if (jsonIgnoredClaims.contains(name)) {
                        return@forEach
                    }
                }
                Text(
                    text = claim.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                RenderClaimValue(claim = claim)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (certSignedDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { certInfoExpanded = !certInfoExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.certificate_info),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                imageVector = if (certInfoExpanded)
                                    Icons.Filled.KeyboardArrowUp
                                else
                                    Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (certInfoExpanded) stringResource(Res.string.collapse) else stringResource(Res.string.expand)
                            )
                        }
                        AnimatedVisibility(visible = certInfoExpanded) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 32.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                )
                            ) {
                                CertificateInfoItem(stringResource(Res.string.cert_signed_date), certSignedDate)
                                CertificateInfoItem(stringResource(Res.string.cert_valid_from), certValidFrom)
                                CertificateInfoItem(stringResource(Res.string.cert_valid_until), certValidUntil)
                                CertificateInfoItem(stringResource(Res.string.cert_expected_update), certExpectedUpdate)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CertificateInfoItem(label: String, instant: Instant?) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = instant?.let { formattedDate(it) }
            ?: AnnotatedString(stringResource(Res.string.not_set)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
}

private fun Claim.isPictureClaim(): Boolean {
    if (attribute?.type == DocumentAttributeType.Picture) return true
    if (this is MdocClaim && value is Bstr && value.asBstr.size > 100) return true
    return false
}

private fun Claim.withPictureAttribute(): Claim {
    if (attribute?.type == DocumentAttributeType.Picture) return this
    if (this is MdocClaim) {
        return copy(
            attribute = DocumentAttribute(
                type = DocumentAttributeType.Picture,
                identifier = dataElementName,
                displayName = displayName,
                description = "",
                icon = null,
                sampleValueMdoc = null,
                sampleValueJson = null,
                parentAttribute = null,
                embeddedAttributes = emptyList()
            )
        )
    }
    return this
}
