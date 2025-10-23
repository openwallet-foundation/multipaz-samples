package org.multipaz.photoididentityreader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.multipaz.compose.certificateviewer.X509CertViewer
import org.multipaz.mdoc.vical.SignedVical
import org.multipaz.mdoc.vical.VicalCertificateInfo
import org.multipaz.trustmanagement.TrustEntryVical
import org.multipaz.trustmanagement.TrustManagerLocal

@Composable
fun VicalEntryViewerScreen(
    builtInTrustManager: TrustManagerLocal,
    userTrustManager: TrustManagerLocal,
    trustManagerId: String,
    entryIndex: Int,
    certificateIndex: Int,
    onBackPressed: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val certificateInfo = remember { mutableStateOf<VicalCertificateInfo?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val trustManager = when (trustManagerId) {
                TRUST_MANAGER_ID_BUILT_IN -> builtInTrustManager
                TRUST_MANAGER_ID_USER -> userTrustManager
                else -> throw IllegalArgumentException()
            }
            val te = trustManager.getEntries()[entryIndex] as TrustEntryVical
            val signedVal = SignedVical.parse(
                encodedSignedVical = te.encodedSignedVical.toByteArray(),
                disableSignatureVerification = true
            )
            certificateInfo.value = signedVal.vical.certificateInfos[certificateIndex]
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = AnnotatedString("VICAL entry"),
                onBackPressed = onBackPressed,
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            certificateInfo.value?.let { ci ->

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ci.RenderIconWithFallback(size = 160.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = ci.displayNameWithFallback,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val entries = mutableListOf<@Composable () -> Unit>()
                    entries.add {
                        EntryItem("Document types", ci.docTypes.joinToString("\n"))
                    }
                    EntryList(
                        title = "VICAL entry",
                        entries = entries
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    X509CertViewer(certificate = ci.certificate)
                }
            }
        }
    }
}