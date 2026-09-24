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
                Text("Higgsfield", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Create, iterate, compare, and save — with every request kept explicit.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
                )
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreationCard("Image", "SOUL, Marketing Studio, Qwen", true, Modifier.weight(1f), onOpenImages)
                        CreationCard("Video", "Seedance, Kling, Cinema Studio, Wan", false, Modifier.weight(1f), onOpenVideos)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreationCard("Image", "SOUL, Marketing Studio, Qwen", true, Modifier.fillMaxWidth(), onOpenImages)
                        CreationCard("Video", "Seedance, Kling, Cinema Studio, Wan", false, Modifier.fillMaxWidth(), onOpenVideos)
                    }
                }
                Text(
                    "Private local build · API generation stays disabled until credentials and a verified adapter are present.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun CreationCard(title: String, subtitle: String, image: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .semantics { stateDescription = "$title creation workspace" }
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp)) {
            Icon(
                if (image) Icons.Rounded.Image else Icons.Rounded.Movie,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 28.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
