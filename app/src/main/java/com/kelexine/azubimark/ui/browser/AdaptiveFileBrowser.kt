package com.kelexine.azubimark.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.kelexine.azubimark.data.model.FileItem

/**
 * Adaptive file list that responds to different screen sizes using WindowSizeClass.
 * 
 * Uses LazyColumn for compact screens and LazyVerticalGrid for larger screens.
 * Ensures proper touch targets (minimum 48dp) on all screen sizes.
 * 
 * Validates: Requirements 5.5
 * 
 * @param files List of files to display
 * @param windowSizeClass The current window size class for responsive layout
 * @param onItemClick Callback when a file item is clicked
 * @param modifier Optional modifier
 */
@Composable
fun AdaptiveFileList(
    files: List<FileItem>,
    windowSizeClass: WindowSizeClass,
    onItemClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthSizeClass = windowSizeClass.widthSizeClass
    
    // Determine layout based on WindowWidthSizeClass
    when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Phone portrait - single column list
            CompactFileList(
                files = files,
                onItemClick = onItemClick,
                modifier = modifier
            )
        }
        WindowWidthSizeClass.Medium -> {
            // Phone landscape, small tablet - 2 column grid
            MediumFileGrid(
                files = files,
                onItemClick = onItemClick,
                modifier = modifier
            )
        }
        WindowWidthSizeClass.Expanded -> {
            // Large tablet, desktop - 3 column grid
            ExpandedFileGrid(
                files = files,
                onItemClick = onItemClick,
                modifier = modifier
            )
        }
    }
}

/**
 * Compact layout for phones in portrait mode.
 * Single column LazyColumn.
 */
@Composable
private fun CompactFileList(
    files: List<FileItem>,
    onItemClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(files, key = { it.uri.toString() }) { fileItem ->
            FileListItem(
                fileItem = fileItem,
                onClick = { onItemClick(fileItem) },
                modifier = Modifier.heightIn(min = 48.dp) // Minimum touch target
            )
        }
    }
}

/**
 * Medium layout for phones in landscape or small tablets.
 * 2 column grid.
 */
@Composable
private fun MediumFileGrid(
    files: List<FileItem>,
    onItemClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { it.uri.toString() }) { fileItem ->
            FileListItem(
                fileItem = fileItem,
                onClick = { onItemClick(fileItem) },
                modifier = Modifier.heightIn(min = 48.dp) // Minimum touch target
            )
        }
    }
}

/**
 * Expanded layout for large tablets and desktops.
 * 3 column grid.
 */
@Composable
private fun ExpandedFileGrid(
    files: List<FileItem>,
    onItemClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(files, key = { it.uri.toString() }) { fileItem ->
            FileListItem(
                fileItem = fileItem,
                onClick = { onItemClick(fileItem) },
                modifier = Modifier.heightIn(min = 48.dp) // Minimum touch target
            )
        }
    }
}

/**
 * Alternative adaptive file list that uses screen width directly.
 * Use this when WindowSizeClass is not available.
 * 
 * @param files List of files to display
 * @param onItemClick Callback when a file item is clicked
 * @param modifier Optional modifier
 */
@Composable
fun AdaptiveFileListByScreenWidth(
    files: List<FileItem>,
    onItemClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    // Determine layout based on screen width
    val columnCount = calculateColumnCount(screenWidthDp)
    val useGridLayout = shouldUseGridLayout(screenWidthDp)

    if (useGridLayout) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files, key = { it.uri.toString() }) { fileItem ->
                FileListItem(
                    fileItem = fileItem,
                    onClick = { onItemClick(fileItem) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(files, key = { it.uri.toString() }) { fileItem ->
                FileListItem(
                    fileItem = fileItem,
                    onClick = { onItemClick(fileItem) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

/**
 * Calculate the number of columns based on screen width.
 * 
 * @param screenWidthDp Screen width in dp
 * @return Number of columns for the grid layout
 */
fun calculateColumnCount(screenWidthDp: Int): Int {
    return when {
        screenWidthDp >= ScreenBreakpoints.EXPANDED -> 3 // Large tablet/desktop
        screenWidthDp >= ScreenBreakpoints.MEDIUM -> 2 // Small tablet
        else -> 1 // Phone
    }
}

/**
 * Determine if grid layout should be used based on screen width.
 * 
 * @param screenWidthDp Screen width in dp
 * @return true if grid layout should be used
 */
fun shouldUseGridLayout(screenWidthDp: Int): Boolean {
    return screenWidthDp >= ScreenBreakpoints.MEDIUM
}

/**
 * Screen size breakpoints for responsive design.
 * Based on Material Design 3 guidelines.
 */
object ScreenBreakpoints {
    const val COMPACT = 0
    const val MEDIUM = 600
    const val EXPANDED = 840
    
    /**
     * Get the screen size category based on width.
     */
    fun getCategory(screenWidthDp: Int): ScreenSizeCategory {
        return when {
            screenWidthDp >= EXPANDED -> ScreenSizeCategory.EXPANDED
            screenWidthDp >= MEDIUM -> ScreenSizeCategory.MEDIUM
            else -> ScreenSizeCategory.COMPACT
        }
    }
    
    /**
     * Convert WindowWidthSizeClass to ScreenSizeCategory.
     */
    fun fromWindowWidthSizeClass(widthSizeClass: WindowWidthSizeClass): ScreenSizeCategory {
        return when (widthSizeClass) {
            WindowWidthSizeClass.Compact -> ScreenSizeCategory.COMPACT
            WindowWidthSizeClass.Medium -> ScreenSizeCategory.MEDIUM
            WindowWidthSizeClass.Expanded -> ScreenSizeCategory.EXPANDED
            else -> ScreenSizeCategory.COMPACT
        }
    }
}

/**
 * Screen size categories for responsive design.
 */
enum class ScreenSizeCategory {
    COMPACT,  // Phone portrait (< 600dp)
    MEDIUM,   // Phone landscape, small tablet (600dp - 840dp)
    EXPANDED  // Large tablet, desktop (> 840dp)
}
