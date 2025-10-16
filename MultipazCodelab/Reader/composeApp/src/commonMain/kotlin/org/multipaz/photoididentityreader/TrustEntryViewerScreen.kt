package org.multipaz.photoididentityreader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.multipaz.compose.cards.InfoCard
import org.multipaz.compose.certificateviewer.X509CertViewer
import org.multipaz.compose.datetime.formattedDateTime
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.vical.SignedVical
import org.multipaz.trustmanagement.TrustEntry
import org.multipaz.trustmanagement.TrustEntryVical
import org.multipaz.trustmanagement.TrustEntryX509Cert
import org.multipaz.trustmanagement.TrustManagerLocal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustEntryViewerScreen(
    builtInTrustManager: TrustManagerLocal,
    userTrustManager: TrustManagerLocal,
    trustManagerId: String,
    entryIndex: Int,
    justImported: Boolean,
    onBackPressed: () -> Unit,
    onEditPressed: (entryIndex: Int) -> Unit,
    onShowVicalEntry: (trustManagerId: String, entryIndex: Int, vicalCertNum: Int) -> Unit,
    onShowCertificate: (certificate: X509Cert) -> Unit,
    onShowCertificateChain: (certificateChain: X509CertChain) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val entry = remember { mutableStateOf<TrustEntry?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val trustManager = when (trustManagerId) {
                TRUST_MANAGER_ID_BUILT_IN -> builtInTrustManager
                TRUST_MANAGER_ID_USER -> userTrustManager
                else -> throw IllegalArgumentException()
            }
            entry.value = trustManager.getEntries()[entryIndex]
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false }
                ) {
                    Text(text = "Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            showDeleteConfirmationDialog = false
                            entry.value?.let {
                                userTrustManager.deleteEntry(it)
                                onBackPressed()
                            }
                        }
                    }
                ) {
                    Text(text = "Delete")
                }
            },
            title = {
                Text(
                    text = when (entry.value!!) {
                        is TrustEntryVical -> "Delete VICAL?"
                        is TrustEntryX509Cert -> "Delete certificate?"
                    }
                )
            },
            text = {
                Text(
                    text = when (entry.value!!) {
                        is TrustEntryVical -> "The VICAL will be permanently deleted. This action cannot be undone"
                        is TrustEntryX509Cert -> "The certificate will be permanently deleted. This action cannot be undone"
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            AppBar(
                title = entry.value?.let {
                    when (it) {
                        is TrustEntryX509Cert -> AnnotatedString("IACA certificate")
                        is TrustEntryVical -> AnnotatedString("VICAL")
                    }
                },
                onBackPressed = onBackPressed,
                actions = {
                    if (trustManagerId == TRUST_MANAGER_ID_USER) {
                        IconButton(
                            onClick = { onEditPressed(entryIndex) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            entry.value?.let { trustEntry ->
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    if (justImported) {
                        InfoCard(
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = when (trustEntry) {
                                    is TrustEntryVical -> {
                                        "This VICAL was just imported. Please check the signer certificate " +
                                                "chain to make sure you trust the provider."
                                    }
                                    is TrustEntryX509Cert -> {
                                        "This IACA certificate was just imported. Please check its fingerprint " +
                                                "to make sure you trust the certificate."
                                    }
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        trustEntry.RenderIconWithFallback(size = 160.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = trustEntry.displayNameWithFallback,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        val entries = mutableListOf<@Composable () -> Unit>()
                        entries.add {
                            EntryItem("Test only",
                                if (trustEntry.metadata.testOnly) {
                                    "Yes"
                                } else {
                                    "No"
                                }
                            )
                        }
                        EntryList(
                            title = null,
                            entries = entries
                        )

                        when (trustEntry) {
                            is TrustEntryX509Cert -> {
                                X509CertViewer(certificate = trustEntry.certificate)
                            }
                            is TrustEntryVical -> {
                                VicalDetails(
                                    trustManagerId = trustManagerId,
                                    entryIndex = entryIndex,
                                    trustEntry = trustEntry,
                                    onShowVicalEntry = onShowVicalEntry,
                                    onShowCertificate = onShowCertificate,
                                    onShowCertificateChain = onShowCertificateChain
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VicalDetails(
    trustManagerId: String,
    entryIndex: Int,
    trustEntry: TrustEntryVical,
    onShowVicalEntry: (trustManagerId: String, entryIndex: Int, vicalCertNum: Int) -> Unit,
    onShowCertificate: (certificate: X509Cert) -> Unit,
    onShowCertificateChain: (certificateChain: X509CertChain) -> Unit,
) {
    val signedVical = remember { SignedVical.parse(
        encodedSignedVical = trustEntry.encodedSignedVical.toByteArray(),
        disableSignatureVerification = true
    )}

    val entries = mutableListOf<@Composable () -> Unit>()
    entries.add {
        EntryItem("Version", signedVical.vical.version)
    }
    entries.add {
        EntryItem("Provider", signedVical.vical.vicalProvider)
    }
    entries.add {
        EntryItem("Issue", signedVical.vical.vicalIssueID.toString())
    }
    entries.add {
        EntryItem("Issued at", formattedDateTime(signedVical.vical.date))
    }
    entries.add {
        EntryItem("Next update", signedVical.vical.nextUpdate?.let {
            formattedDateTime(it)
        } ?: AnnotatedString("-"))
    }
    entries.add { EntryItem("Signer", "Click to view certificate chain",
        modifier = Modifier.clickable {
            onShowCertificateChain(signedVical.vicalProviderCertificateChain)
        })
    }
    EntryList(
        title = "VICAL data",
        entries = entries
    )

    Text(
        modifier = Modifier.padding(vertical = 16.dp),
        text = "IACA certificates",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        signedVical.vical.certificateInfos.forEachIndexed { n, certificateInfo ->
            val isFirst = (n == 0)
            val isLast = (n == signedVical.vical.certificateInfos.size - 1)
            val rounded = 16.dp
            val startRound = if (isFirst) rounded else 0.dp
            val endRound = if (isLast) rounded else 0.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(startRound, startRound, endRound, endRound))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp)
                    .clickable { onShowVicalEntry(trustManagerId, entryIndex, n) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface
                ) {
                    val displayName = certificateInfo.displayNameWithFallback
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            alignment = Alignment.Start
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        certificateInfo.RenderIconWithFallback()
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayName,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "IACA certificate",
                                textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            if (!isLast) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}