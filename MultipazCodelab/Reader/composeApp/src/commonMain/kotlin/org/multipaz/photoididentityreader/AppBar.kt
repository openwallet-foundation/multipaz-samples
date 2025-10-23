package org.multipaz.photoididentityreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import multipazphotoididentityreader.composeapp.generated.resources.Res
import multipazphotoididentityreader.composeapp.generated.resources.back_button_content_description
import org.jetbrains.compose.resources.stringResource

/**
 * The top app bar.
 *
 * @param title the title to show or `null` to not show a title.
 * @param onBackPressed the function to call when the back arrow is pressed or `null` to not show a back arrow.
 * @param onAccountPressed the function to call when the account icon is pressed or `null` to not show an account icon.
 * @param settingsModel the [SettingsModel], must be non-null if onMenuPressed is non-null.
 * @param actions additional actions to show.
 * @param titleClickable true if the title should be clickable.
 * @param onTitleClicked invoked when the title bar is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: AnnotatedString? = null,
    onBackPressed: (() -> Unit)? = null,
    onAccountPressed: (() -> Unit)? = null,
    settingsModel: SettingsModel? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    titleClickable: Boolean = false,
    onTitleClicked: (() -> Unit)? = null
) {
    CenterAlignedTopAppBar(
        title = {
            title?.let {
                Text(
                    modifier = Modifier.clickable(enabled = titleClickable) {
                        onTitleClicked!!.invoke()
                    },
                    text = it
                )
            }
        },
        modifier = Modifier,
        navigationIcon = {
            if (onBackPressed != null) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_button_content_description)
                    )
                }
            }
        },
        actions = {
            if (onAccountPressed != null) {
                IconButton(
                    onClick = onAccountPressed
                ) {
                    val signedIn = settingsModel!!.signedIn.collectAsState()
                    if (signedIn.value != null) {
                        signedIn.value!!.ProfilePicture(size = 32.dp)
                    } else {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                        )
                    }
                }
            }
            actions()
        },
    )
}