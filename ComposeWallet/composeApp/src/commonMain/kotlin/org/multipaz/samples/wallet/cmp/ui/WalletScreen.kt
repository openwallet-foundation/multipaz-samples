package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.add_document_hint
import mpzcmpwallet.composeapp.generated.resources.app_icon
import mpzcmpwallet.composeapp.generated.resources.app_name
import mpzcmpwallet.composeapp.generated.resources.cancel
import mpzcmpwallet.composeapp.generated.resources.no_documents_added
import mpzcmpwallet.composeapp.generated.resources.remove
import mpzcmpwallet.composeapp.generated.resources.remove_digital_id_message
import mpzcmpwallet.composeapp.generated.resources.remove_digital_id_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.compose.document.VerticalDocumentList
import org.multipaz.compose.items.FloatingItemList
import org.multipaz.compose.items.FloatingItemText
import org.multipaz.document.DocumentStore
import org.multipaz.samples.wallet.cmp.SettingsModel
import org.multipaz.util.Logger

private const val TAG = "WalletScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    documentModel: DocumentModel,
    documentStore: DocumentStore,
    settingsModel: SettingsModel,
    onDocumentViewClaims: (documentId: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var focusedDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    val focusedDocument = documentModel.documentInfos.collectAsState().value.find { documentInfo ->
        documentInfo.document.identifier == focusedDocumentId
    }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeightDp = with(density) {
        windowInfo.containerSize.height.toDp()
    }
    val maxCardHeight = screenHeightDp / 3

    // This hooks the back handler so we can close the focused document instead of going back.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = focusedDocumentId != null,
        onBackCompleted = {
            focusedDocumentId = null
        }
    )

    var showRemoveDialog = remember { mutableStateOf(false) }

    if (showRemoveDialog.value) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog.value = false },
            title = {
                Text(
                    text = stringResource(Res.string.remove_digital_id_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.remove_digital_id_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog.value = false
                    coroutineScope.launch {
                        documentStore.deleteDocument(focusedDocumentId!!)
                        focusedDocumentId = null
                    }
                }) {
                    Text(stringResource(Res.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog.value = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (focusedDocument == null) {
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = focusedDocument.document.displayName ?: "Document",  // TODO: translation
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (focusedDocumentId == null) {
                        Icon(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(32.dp),
                            painter = painterResource(Res.drawable.app_icon),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    } else {
                        IconButton(onClick = {
                            // Unfocus focused document instead of going back, like the back handler above
                            focusedDocumentId = null
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerticalDocumentList(
                documentModel = documentModel,
                focusedDocument = focusedDocument,
                allowDocumentReordering = true,
                showStackWhileFocused = false,
                cardMaxHeight = maxCardHeight,
                showDocumentInfo = { documentInfo ->
                    DocumentInfo(
                        documentInfo = documentInfo,
                        showRemoveDialog = showRemoveDialog,
                        onDocumentViewClaims = onDocumentViewClaims
                    )
                },
                emptyDocumentContent = {
                    EmptyWalletState()
                },
                onDocumentReordered = { documentInfo, newIndex ->
                    coroutineScope.launch {
                        try {
                            documentModel.setDocumentPosition(
                                documentInfo = documentInfo,
                                position = newIndex
                            )
                        } catch (e: IllegalArgumentException) {
                            Logger.e(TAG, "Error setting document position", e)
                        }
                    }
                },
                onDocumentFocused = { documentInfo ->
                    focusedDocumentId = documentInfo.document.identifier
                },
                onDocumentFocusedTapped =  { documentInfo ->
                    focusedDocumentId = null
                },
                onDocumentFocusedStackTapped =  { documentInfo -> }
            )
        }
    }
}

@Composable
private fun DocumentInfo(
    documentInfo: DocumentInfo,
    showRemoveDialog: MutableState<Boolean>,
    onDocumentViewClaims: (documentId: String) -> Unit,
) {
    val iconSize = 32.dp
    Column(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        FloatingItemList {
            FloatingItemText(
                text = "View and manage activity",
                image = {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.Outlined.History,
                        contentDescription = null
                    )
                }
            )
            FloatingItemText(
                modifier = Modifier.clickable {
                    onDocumentViewClaims(documentInfo.document.identifier)
                },
                text = "${documentInfo.document.typeDisplayName} info",
                image = {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null
                    )
                }
            )
            FloatingItemText(
                text = "How to use this document",
                image = {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null
                    )
                }
            )
            FloatingItemText(
                text = "Issuer website",
                image = {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null
                    )
                }
            )
            FloatingItemText(
                modifier = Modifier.clickable {
                    showRemoveDialog.value = true
                },
                text = "Remove",
                image = {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyWalletState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.no_documents_added),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(Res.string.add_document_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}



