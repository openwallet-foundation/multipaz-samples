package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.add_document_hint
import mpzcmpwallet.composeapp.generated.resources.app_icon
import mpzcmpwallet.composeapp.generated.resources.app_name
import mpzcmpwallet.composeapp.generated.resources.carousel_drag_to_reader
import mpzcmpwallet.composeapp.generated.resources.hold_to_reader
import mpzcmpwallet.composeapp.generated.resources.no_documents_added
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.document.DocumentCarousel
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.samples.wallet.cmp.SettingsModel
import org.multipaz.util.Logger

private const val TAG = "WalletScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    documentModel: DocumentModel,
    settingsModel: SettingsModel,
    onDocumentSelected: (DocumentInfo) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    Icon(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp),
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
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
            DocumentCarousel(
                modifier = Modifier.fillMaxWidth(),
                documentModel = documentModel,
                initialDocumentId = settingsModel.currentlyFocusedDocumentId.value,
                allowReordering = true,
                onDocumentReordered = { documentInfo, oldPos, newPos ->
                    coroutineScope.launch {
                        try {
                            documentModel.setDocumentPosition(
                                documentInfo = documentInfo,
                                position = newPos
                            )
                        } catch (e: IllegalArgumentException) {
                            Logger.e(TAG, "Error setting document position", e)
                        }
                    }
                },
                onDocumentFocused = { documentInfo ->
                    settingsModel.currentlyFocusedDocumentId.value = documentInfo.document.identifier
                },
                selectedDocumentInfo = { documentInfo, index, total ->
                    if (documentInfo != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Contactless,
                                contentDescription = null,
                                modifier = Modifier.size(25.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.hold_to_reader),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.carousel_drag_to_reader),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onDocumentClicked = { documentInfo ->
                    onDocumentSelected(documentInfo)
                },
                emptyDocumentContent = {
                    EmptyWalletState()
                },
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






