package me.kelexine.azubimark

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.regex.Pattern

class DocumentOutline(
    private val context: Context,
    private val containerView: LinearLayout,
    private val onHeadingClick: (HeadingItem) -> Unit
) {
    
    data class HeadingItem(
        val level: Int,
        val text: String,
        val position: Int,
        val id: String = generateId(text)
    ) {
        companion object {
            private fun generateId(text: String): String {
                return text.lowercase()
                    .replace(Regex("[^a-z0-9\\s]"), "")
                    .replace(Regex("\\s+"), "-")
                    .trim('-')
            }
        }
    }
    
    private val headings = mutableListOf<HeadingItem>()
    private val headingViews = mutableListOf<MaterialButton>()
    private var activeHeadingIndex = -1
    
    fun generateOutline(markdownContent: String) {
        headings.clear()
        headingViews.clear()
        containerView.removeAllViews()
        
        // Parse headings from markdown content
        parseHeadings(markdownContent)
        
        // Create UI for headings
        createOutlineUI()
    }
    
    private fun parseHeadings(content: String) {
        val headingPattern = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE)
        val matcher = headingPattern.matcher(content)
        
        while (matcher.find()) {
            val level = matcher.group(1)?.length ?: 0
            val text = matcher.group(2)?.trim() ?: ""
            val position = matcher.start()
            
            if (level in 1..6 && text.isNotEmpty()) {
                headings.add(HeadingItem(level, text, position))
            }
        }
    }
    
    private fun createOutlineUI() {
        if (headings.isEmpty()) {
            showNoHeadingsMessage()
            return
        }
        
        headings.forEachIndexed { index, heading ->
            val button = createHeadingButton(heading, index)
            headingViews.add(button)
            containerView.addView(button)
        }
    }
    
    private fun createHeadingButton(heading: HeadingItem, index: Int): MaterialButton {
        val button = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = heading.text
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Add indentation based on heading level
                val baseMargin = context.resources.getDimensionPixelSize(R.dimen.outline_indent_base)
                val stepMargin = context.resources.getDimensionPixelSize(R.dimen.outline_indent_step)
                val leftMargin = baseMargin + (heading.level - 1) * stepMargin
                setMargins(leftMargin, 0, baseMargin, 4)
            }
            
            // Set text appearance based on heading level
            setTextAppearance(getTextAppearanceForLevel(heading.level))
            
            // Set click listener
            setOnClickListener {
                onHeadingClick(heading)
                setActiveHeading(index)
            }
            
            // Set initial styling
            updateButtonStyle(false)
        }
        
        return button
    }
    
    private fun getTextAppearanceForLevel(level: Int): Int {
        return when (level) {
            1 -> R.style.TextAppearance_AzubiMark_OutlineItem_H1
            2 -> R.style.TextAppearance_AzubiMark_OutlineItem_H2
            3 -> R.style.TextAppearance_AzubiMark_OutlineItem_H3
            else -> R.style.TextAppearance_AzubiMark_OutlineItem
        }
    }
    
    private fun showNoHeadingsMessage() {
        val textView = TextView(context).apply {
            text = context.getString(R.string.no_headings_found)
            setTextAppearance(R.style.TextAppearance_AzubiMark_BodyMedium)
            setTextColor(ContextCompat.getColor(context, com.google.android.material.R.color.material_on_surface_emphasis_medium))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = context.resources.getDimensionPixelSize(R.dimen.spacing_lg)
                setMargins(margin, margin, margin, margin)
            }
        }
        containerView.addView(textView)
    }
    
    fun updateActiveSection(scrollY: Int) {
        // Find the heading that corresponds to the current scroll position
        var newActiveIndex = -1
        
        for (i in headings.indices.reversed()) {
            // This is a simplified calculation - in a real implementation,
            // you'd need to map heading positions to actual pixel positions
            val approximatePosition = headings[i].position / 10 // Rough estimate
            if (scrollY >= approximatePosition) {
                newActiveIndex = i
                break
            }
        }
        
        if (newActiveIndex != activeHeadingIndex) {
            setActiveHeading(newActiveIndex)
        }
    }
    
    private fun setActiveHeading(index: Int) {
        // Update previous active heading
        if (activeHeadingIndex >= 0 && activeHeadingIndex < headingViews.size) {
            headingViews[activeHeadingIndex].updateButtonStyle(false)
        }
        
        // Update new active heading
        activeHeadingIndex = index
        if (activeHeadingIndex >= 0 && activeHeadingIndex < headingViews.size) {
            headingViews[activeHeadingIndex].updateButtonStyle(true)
        }
    }
    
    private fun MaterialButton.updateButtonStyle(isActive: Boolean) {
        if (isActive) {
            backgroundTintList = ContextCompat.getColorStateList(context, com.google.android.material.R.color.material_dynamic_primary20)
            setTextColor(ContextCompat.getColor(context, com.google.android.material.R.color.material_dynamic_primary10))
            strokeColor = ContextCompat.getColorStateList(context, com.google.android.material.R.color.material_dynamic_primary50)
        } else {
            backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.transparent)
            setTextColor(ContextCompat.getColor(context, com.google.android.material.R.color.material_on_surface_emphasis_high_type))
            strokeColor = ContextCompat.getColorStateList(context, android.R.color.transparent)
        }
    }
    
    fun getHeadingCount(): Int = headings.size
    
    fun getHeadings(): List<HeadingItem> = headings.toList()
}