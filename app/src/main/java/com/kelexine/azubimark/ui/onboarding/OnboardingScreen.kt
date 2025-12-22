package com.kelexine.azubimark.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing an onboarding page.
 */
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * Onboarding screen with swipeable pages introducing app features.
 * 
 * @param onComplete Callback when onboarding is completed
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.EditNote,
            title = "Welcome to AzubiMark",
            description = "A beautiful, modern Markdown viewer designed for Android with Material You design."
        ),
        OnboardingPage(
            icon = Icons.Filled.TableChart,
            title = "Rich Markdown Support",
            description = "View tables, task lists, code with syntax highlighting, and more. All rendered beautifully."
        ),
        OnboardingPage(
            icon = Icons.Filled.CheckBox,
            title = "Interactive Task Lists",
            description = "Toggle checkboxes in your Markdown files. Changes are session-only and never modify your files."
        ),
        OnboardingPage(
            icon = Icons.Filled.DarkMode,
            title = "Material You Theming",
            description = "Adapts to your system colors on Android 12+. Choose between Light, Dark, or System themes."
        ),
        OnboardingPage(
            icon = Icons.Filled.FolderOpen,
            title = "Easy File Access",
            description = "Browse your device storage or open Markdown files directly from any file manager."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onComplete) {
                Text("Skip")
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text("Back")
                }
            }
            
            if (pagerState.currentPage == 0) {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Next/Get Started button
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            ) {
                Text(
                    if (pagerState.currentPage == pages.size - 1) "Get Started"
                    else "Next"
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
