package me.kelexine.azubimark

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.PreferenceManager

/**
 * Helper class to improve accessibility and usability for users with disabilities
 */
object AccessibilityHelper {
    
    /**
     * Check if accessibility services are enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled
    }
    
    /**
     * Check if TalkBack or similar screen reader is enabled
     */
    fun isScreenReaderEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Set up enhanced accessibility for a view
     */
    fun setupAccessibility(view: View, contentDescription: String? = null, hint: String? = null) {
        contentDescription?.let { view.contentDescription = it }
        
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                hint?.let { info.hintText = it }
                
                // Add custom actions if needed
                when (view.javaClass.simpleName) {
                    "FloatingActionButton" -> {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
                    }
                    "RecyclerView" -> {
                        info.className = "android.widget.ListView"
                    }
                }
            }
        })
    }
    
    /**
     * Announce text to screen readers
     */
    fun announceText(context: Context, text: String) {
        if (isScreenReaderEnabled(context)) {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            if (accessibilityManager.isEnabled) {
                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                event.text.add(text)
                accessibilityManager.sendAccessibilityEvent(event)
            }
        }
    }
    
    /**
     * Get recommended touch target size based on user preferences
     */
    fun getRecommendedTouchTargetSize(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val largeTouchTargets = prefs.getBoolean("large_touch_targets", false)
        
        return if (largeTouchTargets) {
            56 // dp - larger touch targets for accessibility
        } else {
            48 // dp - standard touch target size
        }
    }
    
    /**
     * Check if high contrast mode is enabled
     */
    fun isHighContrastEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean("high_contrast", false)
    }
    
    /**
     * Apply accessibility-friendly text sizing
     */
    fun getAccessibleTextSize(context: Context, baseSize: Float): Float {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val fontSize = prefs.getInt("font_size", 17)
        
        // Scale based on user preference and system settings
        val systemScale = context.resources.configuration.fontScale
        return baseSize * (fontSize / 16f) * systemScale
    }
    
    /**
     * Set up proper focus handling for keyboard navigation
     */
    fun setupKeyboardNavigation(vararg views: View) {
        for (i in views.indices) {
            val currentView = views[i]
            val nextView = if (i < views.size - 1) views[i + 1] else null
            
            currentView.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                
                nextView?.let { next ->
                    nextFocusDownId = next.id
                    nextFocusForwardId = next.id
                }
                
                if (i > 0) {
                    val prevView = views[i - 1]
                    nextFocusUpId = prevView.id
                    nextFocusDownId = prevView.id
                }
            }
        }
    }
    
    /**
     * Create accessibility-friendly content descriptions for markdown elements
     */
    fun getMarkdownElementDescription(elementType: String, content: String): String {
        return when (elementType.lowercase()) {
            "heading1", "h1" -> "Heading level 1: $content"
            "heading2", "h2" -> "Heading level 2: $content"
            "heading3", "h3" -> "Heading level 3: $content"
            "heading4", "h4" -> "Heading level 4: $content"
            "heading5", "h5" -> "Heading level 5: $content"
            "heading6", "h6" -> "Heading level 6: $content"
            "link" -> "Link: $content"
            "image" -> "Image: $content"
            "code" -> "Code: $content"
            "codeblock" -> "Code block: $content"
            "list" -> "List with ${content.split('\n').size} items"
            "table" -> "Table with data"
            "blockquote" -> "Quote: $content"
            "strong", "bold" -> "Bold text: $content"
            "emphasis", "italic" -> "Italic text: $content"
            "strikethrough" -> "Strikethrough text: $content"
            else -> content
        }
    }
    
    /**
     * Set up voice feedback for actions
     */
    fun setupVoiceFeedback(context: Context, view: View, actionDescription: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val voiceFeedbackEnabled = prefs.getBoolean("voice_feedback", false)
        
        if (voiceFeedbackEnabled) {
            view.setOnClickListener {
                announceText(context, actionDescription)
            }
        }
    }
    
    /**
     * Create semantic markup for screen readers
     */
    fun createSemanticMarkup(content: String): String {
        return content
            .replace(Regex("^# (.+)$", RegexOption.MULTILINE)) { 
                "<h1>${it.groupValues[1]}</h1>" 
            }
            .replace(Regex("^## (.+)$", RegexOption.MULTILINE)) { 
                "<h2>${it.groupValues[1]}</h2>" 
            }
            .replace(Regex("^### (.+)$", RegexOption.MULTILINE)) { 
                "<h3>${it.groupValues[1]}</h3>" 
            }
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { 
                "<strong>${it.groupValues[1]}</strong>" 
            }
            .replace(Regex("\\*(.+?)\\*")) { 
                "<em>${it.groupValues[1]}</em>" 
            }
            .replace(Regex("\\[(.+?)\\]\\((.+?)\\)")) { 
                "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>" 
            }
    }
    
    /**
     * Get contrast ratio between two colors
     */
    fun getContrastRatio(color1: Int, color2: Int): Double {
        val luminance1 = getLuminance(color1)
        val luminance2 = getLuminance(color2)
        
        val brighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)
        
        return (brighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Calculate relative luminance of a color
     */
    private fun getLuminance(color: Int): Double {
        val red = android.graphics.Color.red(color) / 255.0
        val green = android.graphics.Color.green(color) / 255.0
        val blue = android.graphics.Color.blue(color) / 255.0
        
        fun sRGBToLinear(value: Double): Double {
            return if (value <= 0.03928) {
                value / 12.92
            } else {
                Math.pow((value + 0.055) / 1.055, 2.4)
            }
        }
        
        val r = sRGBToLinear(red)
        val g = sRGBToLinear(green)
        val b = sRGBToLinear(blue)
        
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    
    /**
     * Check if color combination meets accessibility standards
     */
    fun meetsAccessibilityStandards(foreground: Int, background: Int, largeText: Boolean = false): Boolean {
        val contrast = getContrastRatio(foreground, background)
        val requiredRatio = if (largeText) 3.0 else 4.5
        
        return contrast >= requiredRatio
    }
}