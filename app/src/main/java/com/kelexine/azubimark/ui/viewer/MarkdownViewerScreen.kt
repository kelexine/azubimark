package com.kelexine.azubimark.ui.viewer

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kelexine.azubimark.domain.markdown.MarkwonRenderer
import io.noties.markwon.Markwon

/**
 * Markdown viewer screen composable.
 * 
 * Displays parsed Markdown content with interactive task lists,
 * syntax highlighting, and proper error handling.
 * 
 * Validates: Requirements 4.4
 * 
 * @param viewModel The MarkdownViewerViewModel managing the screen state
 * @param onNavigateBack Callback when back navigation is requested
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownViewerScreen(
    viewModel: MarkdownViewerViewModel,
    markwonRenderer: MarkwonRenderer,
    onNavigateBack: () -> Unit,
    onRequestPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState(uiState.scrollPosition)
    
    // Track scroll position changes
    LaunchedEffect(scrollState.value) {
        viewModel.updateScrollPosition(scrollState.value)
    }

    // Restore scroll position when file changes
    LaunchedEffect(uiState.currentFile?.uri) {
        if (uiState.scrollPosition > 0) {
            scrollState.scrollTo(uiState.scrollPosition)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentFile?.name ?: "Markdown Viewer",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (uiState.error != null) {
                        IconButton(onClick = { viewModel.retry() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.retry() },
                        onDismiss = { viewModel.clearError() },
                        onRequestPermission = onRequestPermission
                    )
                }
                uiState.currentFile != null -> {
                    MarkdownContent(
                        content = uiState.currentFile!!.content,
                        markwon = markwonRenderer.getMarkwon(),
                        taskStates = viewModel.getTaskStates(),
                        onTaskToggle = { position, isChecked ->
                            viewModel.toggleTask(position, isChecked)
                        },
                        scrollState = scrollState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    EmptyContent()
                }
            }
        }
    }
}


/**
 * Markdown content display using AndroidView with Markwon.
 */
@Composable
private fun MarkdownContent(
    content: String,
    markwon: Markwon,
    taskStates: Map<Int, Boolean>,
    onTaskToggle: (Int, Boolean) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    
    // Parse markdown content
    val spanned = remember(content) {
        markwon.toMarkdown(content)
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                TextView(ctx).apply {
                    // Enable link clicking
                    movementMethod = LinkMovementMethod.getInstance()
                    
                    // Apply Material theme colors
                    setTextColor(colorScheme.onSurface.toArgb())
                    setLinkTextColor(colorScheme.primary.toArgb())
                    
                    // Set text appearance
                    textSize = 16f
                    setLineSpacing(4f, 1.2f)
                }
            },
            update = { textView ->
                // Update colors when theme changes
                textView.setTextColor(colorScheme.onSurface.toArgb())
                textView.setLinkTextColor(colorScheme.primary.toArgb())
                
                // Set the markdown content
                markwon.setParsedMarkdown(textView, spanned)
            },
            modifier = Modifier.fillMaxWidth()
        )
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading file...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error content with retry and dismiss options.
 */
@Composable
private fun ErrorContent(
    error: FileError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val (title, message, icon) = when (error) {
        is FileError.FileNotFound -> Triple(
            "File Not Found",
            error.message,
            Icons.Filled.Description
        )
        is FileError.PermissionDenied -> Triple(
            "Permission Denied",
            "Unable to access the file. Please grant storage permissions.",
            Icons.Filled.Lock
        )
        is FileError.EncodingError -> Triple(
            "Encoding Error",
            "Unable to read the file. The file may be corrupted or use an unsupported encoding.",
            Icons.Filled.EditNote
        )
        is FileError.ParseError -> Triple(
            "Parse Error",
            "Unable to parse the Markdown content. ${error.message}",
            Icons.Filled.Warning
        )
        is FileError.Unknown -> Triple(
            "Error",
            error.message,
            Icons.Filled.Close
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
            
            if (error is FileError.PermissionDenied) {
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            } else {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Empty content placeholder.
 */
@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No file selected",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error dialog composable for displaying file errors.
 */
@Composable
fun ErrorDialog(
    error: FileError,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val (title, message) = when (error) {
        is FileError.FileNotFound -> "File Not Found" to error.message
        is FileError.PermissionDenied -> "Permission Denied" to "Unable to access the file. Please grant storage permissions."
        is FileError.EncodingError -> "Encoding Error" to "Unable to read the file. The file may be corrupted or use an unsupported encoding."
        is FileError.ParseError -> "Parse Error" to "Unable to parse the Markdown content. ${error.message}"
        is FileError.Unknown -> "Error" to error.message
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

/**
 * Fallback content for unsupported or problematic files.
 * 
 * Displays the raw content with a warning message when Markdown
 * parsing fails but the file content is still readable.
 * 
 * Validates: Requirements 7.3, 7.4
 */
@Composable
fun FallbackContent(
    rawContent: String,
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Warning card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Content Warning",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Raw content display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Raw Content:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rawContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Permission request content for when storage access is denied.
 * 
 * Provides guidance on how to grant permissions.
 * 
 * Validates: Requirements 7.4
 */
@Composable
fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Storage Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AzubiMark needs permission to access your files to display Markdown documents.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }
}

/**
 * Get a user-friendly error message for display.
 */
fun getErrorDisplayInfo(error: FileError): ErrorDisplayInfo {
    return when (error) {
        is FileError.FileNotFound -> ErrorDisplayInfo(
            title = "File Not Found",
            message = "The requested file could not be found. It may have been moved or deleted.",
            icon = Icons.Filled.Description,
            canRetry = true
        )
        is FileError.PermissionDenied -> ErrorDisplayInfo(
            title = "Permission Denied",
            message = "Unable to access the file. Please grant storage permissions in your device settings.",
            icon = Icons.Filled.Lock,
            canRetry = true
        )
        is FileError.EncodingError -> ErrorDisplayInfo(
            title = "Encoding Error",
            message = "Unable to read the file. The file may be corrupted or use an unsupported text encoding (only UTF-8 and UTF-16 are supported).",
            icon = Icons.Filled.EditNote,
            canRetry = true
        )
        is FileError.ParseError -> ErrorDisplayInfo(
            title = "Parse Error",
            message = "Unable to parse the Markdown content. The file may contain invalid formatting.",
            icon = Icons.Filled.Warning,
            canRetry = true
        )
        is FileError.Unknown -> ErrorDisplayInfo(
            title = "Unexpected Error",
            message = error.message.ifEmpty { "An unexpected error occurred while loading the file." },
            icon = Icons.Filled.Close,
            canRetry = true
        )
    }
}

/**
 * Data class containing error display information.
 */
data class ErrorDisplayInfo(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val canRetry: Boolean
)
