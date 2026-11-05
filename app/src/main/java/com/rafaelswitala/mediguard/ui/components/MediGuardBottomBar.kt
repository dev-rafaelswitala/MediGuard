package com.rafaelswitala.mediguard.ui.components

/**
 * Gemeinsame untere Navigationsleiste mit 5 Tabs.
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings

enum class MediGuardTab {
    HOME,
    MEDICATIONS,
    HISTORY,
    PROFILE,
    MORE
}

@Composable
fun MediGuardBottomBar(
    selected: MediGuardTab,
    onHome: () -> Unit,
    onMedications: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
    onMore: () -> Unit
) {
    val strings = LocalAppStrings.current
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = selected == MediGuardTab.HOME,
            onClick = onHome,
            icon = { Icon(Icons.Default.Home, contentDescription = strings.home) },
            label = { Text(strings.home) }
        )
        NavigationBarItem(
            selected = selected == MediGuardTab.MEDICATIONS,
            onClick = onMedications,
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = strings.medications) },
            label = { Text(strings.medications) }
        )
        NavigationBarItem(
            selected = selected == MediGuardTab.HISTORY,
            onClick = onHistory,
            icon = { Icon(Icons.Default.DateRange, contentDescription = strings.history) },
            label = {
                Text(strings.history, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        )
        NavigationBarItem(
            selected = selected == MediGuardTab.PROFILE,
            onClick = onProfile,
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = strings.profile) },
            label = {
                Text(strings.profile, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        )
        NavigationBarItem(
            selected = selected == MediGuardTab.MORE,
            onClick = onMore,
            icon = { Icon(Icons.Default.Settings, contentDescription = strings.more) },
            label = {
                Text(strings.more, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        )
    }
}
