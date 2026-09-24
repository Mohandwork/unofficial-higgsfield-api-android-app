package com.higgsfield.mobile.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.higgsfield.mobile.R

@Composable
fun HomeScreen(onOpenImages: () -> Unit, onOpenVideos: () -> Unit) {
    Scaffold { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 24.dp),
        ) {
            val wide = maxWidth >= 700.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
                Text(
                    stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
                )
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreationCard(R.string.home_image, R.string.home_image_models, true, Modifier.weight(1f), onOpenImages)
                        CreationCard(R.string.home_video, R.string.home_video_models, false, Modifier.weight(1f), onOpenVideos)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreationCard(R.string.home_image, R.string.home_image_models, true, Modifier.fillMaxWidth(), onOpenImages)
                        CreationCard(R.string.home_video, R.string.home_video_models, false, Modifier.fillMaxWidth(), onOpenVideos)
                    }
                }
                Text(
                    stringResource(R.string.home_private_build),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun CreationCard(@StringRes title: Int, @StringRes subtitle: Int, image: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val titleText = stringResource(title)
    val creationWorkspaceDescription = stringResource(R.string.creation_workspace, titleText)
    Card(
        modifier = modifier
            .semantics { stateDescription = creationWorkspaceDescription }
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp)) {
            Icon(
                if (image) Icons.Rounded.Image else Icons.Rounded.Movie,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text(titleText, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp))
            Text(stringResource(subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
