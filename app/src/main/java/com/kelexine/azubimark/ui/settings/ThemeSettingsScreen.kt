package com.kelexine.azubimark.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kelexine.azubimark.data.model.ThemeType
import com.kelexine.azubimark.domain.theme.ThemeManager
import kotlinx.coroutines.launch

/**
 * Theme settings screen for customizing app appearance.
 * 
 * Validates: Requirements 3.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    themeManager: ThemeManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme by themeManager.currentTheme.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Theme Selection Section
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    ThemeOption(
                        title = "Light",
                        description = "Always use light theme",
                        icon = Icons.Default.LightMode,
                        selected = currentTheme.type == ThemeType.LIGHT,
                        onClick = {
                            scope.launch {
                                themeManager.setTheme(ThemeType.LIGHT)
                            }
                        }
                    )

                    Divider()

                    ThemeOption(
                        title = "Dark",
                        description = "Always use dark theme",
                        icon = Icons.Default.DarkMode,
                        selected = currentTheme.type == ThemeType.DARK,
                        onClick = {
                            scope.launch {
                                themeManager.setTheme(ThemeType.DARK)
                            }
                        }
                    )

                    Divider()

                    ThemeOption(
                        title = "System",
                        description = "Follow system theme settings",
                        icon = Icons.Default.Settings,
                        selected = currentTheme.type == ThemeType.SYSTEM,
                        onClick = {
                            scope.launch {
                                themeManager.setTheme(ThemeType.SYSTEM)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Colors Section (Material You)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    text = "Material You",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Colors",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Use colors from your wallpaper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = currentTheme.isDynamicColors,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    themeManager.setDynamicColors(enabled)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info card about Material You
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "ℹ️",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Material You automatically adapts the app's colors to match your device's wallpaper, creating a personalized experience.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Theme Preview Section
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            ThemePreviewCard()
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = null // Handled by Row's selectable
        )
    }
}

@Composable
private fun ThemePreviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sample Content",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This is how text will appear with the current theme settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { }) {
                    Text("Primary")
                }
                OutlinedButton(onClick = { }) {
                    Text("Outlined")
                }
                TextButton(onClick = { }) {
                    Text("Text")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorSwatch("Primary", MaterialTheme.colorScheme.primary)
                ColorSwatch("Secondary", MaterialTheme.colorScheme.secondary)
                ColorSwatch("Tertiary", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.small,
            color = color
        ) {}
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
