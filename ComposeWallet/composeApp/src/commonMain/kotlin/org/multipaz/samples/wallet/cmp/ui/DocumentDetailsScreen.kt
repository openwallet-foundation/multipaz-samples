package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import mpzcmpwallet.composeapp.generated.resources.Res
import mpzcmpwallet.composeapp.generated.resources.activity_history_off
import mpzcmpwallet.composeapp.generated.resources.back
import mpzcmpwallet.composeapp.generated.resources.cancel
import mpzcmpwallet.composeapp.generated.resources.how_to_use_id
import mpzcmpwallet.composeapp.generated.resources.id_info_share_message
import mpzcmpwallet.composeapp.generated.resources.id_issuer_message
import mpzcmpwallet.composeapp.generated.resources.remove
import mpzcmpwallet.composeapp.generated.resources.remove_digital_id_message
import mpzcmpwallet.composeapp.generated.resources.remove_digital_id_title
import mpzcmpwallet.composeapp.generated.resources.type_info
import mpzcmpwallet.composeapp.generated.resources.type_website
import mpzcmpwallet.composeapp.generated.resources.view_and_manage_activity
import mpzcmpwallet.composeapp.generated.resources.youre_in_control
import org.jetbrains.compose.resources.stringResource
import org.multipaz.compose.document.DocumentModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    documentId: String,
    documentModel: DocumentModel,
    onBack: () -> Unit,
    onPersonalIdInfoClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val documentInfo = documentModel.documentInfos.collectAsState().value.find {
        it.document.identifier == documentId
    }
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
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
                    showRemoveDialog = false
                    onRemove()
                }) {
                    Text(stringResource(Res.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        documentInfo?.let { info ->
            val displayName = info.document.displayName.orEmpty()
            val typeDisplayName = info.document.typeDisplayName.orEmpty()

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Image(
                    bitmap = info.cardArt,
                    contentDescription = typeDisplayName,
                    modifier = Modifier
                        .size(width = 100.dp, height = 63.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = typeDisplayName.ifEmpty { displayName },
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                DetailMenuItem(
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    title = stringResource(Res.string.view_and_manage_activity),
                    subtitle = stringResource(Res.string.activity_history_off),
                    onClick = { }
                )

                DetailMenuItem(
                    icon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                    title = stringResource(Res.string.type_info, typeDisplayName),
                    onClick = onPersonalIdInfoClick
                )

                DetailMenuItem(
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null
                        )
                    },
                    title = stringResource(Res.string.how_to_use_id),
                    onClick = { }
                )

                DetailMenuItem(
                    icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    title = stringResource(Res.string.type_website, typeDisplayName),
                    onClick = { }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.youre_in_control),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.id_info_share_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = { showRemoveDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(Res.string.remove),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.id_issuer_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
