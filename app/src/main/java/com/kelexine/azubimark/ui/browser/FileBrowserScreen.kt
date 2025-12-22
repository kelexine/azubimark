package com.kelexine.azubimark.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kelexine.azubimark.data.model.FileItem

/**
 * File browser screen composable.
 * 
 * Displays a navigable file system interface showing directories and Markdown files.
 * Provides breadcrumb navigation and back button functionality.
 * 
 * Validates: Requirements 2.1, 2.5
 * 
 * @param viewModel The FileBrowserViewModel managing the screen state
 * @param onFileSelected Callback when a Markdown file is selected
 * @param onNavigateToAbout Callback to navigate to About screen
 * @param onNavigateToSettings Callback to navigate to Settings screen
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onFileSelected: (FileItem) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.currentDirectoryName == "AzubiMark") "Browse Files" else uiState.currentDirectoryName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.canNavigateBack) {
                                viewModel.navigateBack()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Theme") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAbout()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
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
        ) {
            // Breadcrumb navigation
            BreadcrumbNavigation(
                breadcrumbs = uiState.breadcrumbs,
                onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) }
            )

            // Content
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.files.isEmpty() -> {
                    EmptyContent()
                }
                else -> {
                    FileList(
                        files = uiState.files,
                        onItemClick = { fileItem ->
                            if (fileItem.isDirectory) {
                                viewModel.navigateToDirectory(fileItem)
                            } else {
                                onFileSelected(fileItem)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Breadcrumb navigation component.
 */
@Composable
private fun BreadcrumbNavigation(
    breadcrumbs: List<BreadcrumbItem>,
    onBreadcrumbClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(breadcrumbs) { index, breadcrumb ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(
                onClick = { onBreadcrumbClick(breadcrumb.index) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = breadcrumb.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (index == breadcrumbs.lastIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * File list component using LazyColumn.
 */
@Composable
private fun FileList(
    files: List<FileItem>,
    onItemClick: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(files, key = { it.uri.toString() }) { fileItem ->
            FileListItem(
                fileItem = fileItem,
                onClick = { onItemClick(fileItem) }
            )
        }
    }
}

/**
 * Loading content placeholder.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error content with retry option.
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Empty directory content.
 */
@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No Markdown files found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
