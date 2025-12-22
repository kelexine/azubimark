package com.kelexine.azubimark.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape scale for AzubiMark.
 * 
 * Follows Material Design 3 shape guidelines with rounded corners
 * for a modern, friendly appearance.
 */
val Shapes = Shapes(
    // Extra small - for chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for buttons, text fields
    small = RoundedCornerShape(8.dp),
    
    // Medium - for cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - for bottom sheets, large cards
    large = RoundedCornerShape(16.dp),
    
    // Extra large - for full-screen dialogs
    extraLarge = RoundedCornerShape(28.dp)
)
