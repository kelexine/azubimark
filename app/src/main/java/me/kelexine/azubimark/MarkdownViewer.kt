package me.kelexine.azubimark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.widget.TextView
import android.widget.Toast
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.RenderProps
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.Syntax
import io.noties.prism4j.annotations.PrismBundle
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading

// Generated into the same package by prism4j-bundler
import me.kelexine.azubimark.Prism4jGrammarLocatorDef

class MarkdownViewer(
    private val context: Context,
    private val textView: TextView
) {
    private val themeManager = EnhancedThemeManager(context)
    private val prism4j = Prism4j(Prism4jGrammarLocatorDef())

    private val markwon = Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(createEnhancedTablePlugin())
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(createSyntaxHighlightPlugin())
        .usePlugin(createEnhancedTypographyPlugin())
        .usePlugin(createCodeBlockEnhancementPlugin())
        .build()

    private fun createEnhancedTablePlugin(): TablePlugin {
        val tableTheme = TableTheme.Builder()
            .tableBorderColor(ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorOutline))
            .tableBorderWidth(context.resources.getDimensionPixelSize(R.dimen.spacing_xs))
            .tableCellPadding(context.resources.getDimensionPixelSize(R.dimen.spacing_md))
            .tableHeaderRowBackgroundColor(ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorSurfaceVariant))
            .tableEvenRowBackgroundColor(ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorSurface))
            .tableOddRowBackgroundColor(ThemeUtils.adjustAlpha(
                ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorSurfaceVariant), 0.3f))
            .build()
        
        return TablePlugin.create(tableTheme)
    }

    private fun createSyntaxHighlightPlugin(): SyntaxHighlightPlugin =
        SyntaxHighlightPlugin.create(
            prism4j,
            when (themeManager.getCurrentTheme()) {
                ThemeManager.THEME_LIGHT -> EnhancedLightSyntaxTheme(context)
                ThemeManager.THEME_DARK  -> EnhancedDarkSyntaxTheme(context)
                else                    -> MaterialYouSyntaxTheme(context)
            }
        )

    private fun createEnhancedTypographyPlugin(): AbstractMarkwonPlugin =
        object : AbstractMarkwonPlugin() {
            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder.setFactory(Heading::class.java) { _, props ->
                    val level = io.noties.markwon.core.CoreProps.HEADING_LEVEL.require(props)
                    createHeadingSpans(level)
                }
                
                builder.setFactory(Code::class.java) { _, _ ->
                    createInlineCodeSpans()
                }
            }
        }
    
    private fun createCodeBlockEnhancementPlugin(): AbstractMarkwonPlugin =
        object : AbstractMarkwonPlugin() {
            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder.setFactory(FencedCodeBlock::class.java) { _, props ->
                    createCodeBlockSpans()
                }
            }
        }

    private fun createHeadingSpans(level: Int): Array<Any> {
        val spans = mutableListOf<Any>()
        
        // Size and weight based on heading level
        val sizeRatio = when (level) {
            1 -> 1.8f
            2 -> 1.6f
            3 -> 1.4f
            4 -> 1.2f
            5 -> 1.1f
            6 -> 1.05f
            else -> 1.0f
        }
        
        spans.add(RelativeSizeSpan(sizeRatio))
        
        // Bold for higher level headings
        if (level <= 3) {
            spans.add(StyleSpan(Typeface.BOLD))
        }
        
        // Color based on level
        val headingColor = when (level) {
            1, 2 -> ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorPrimary)
            3, 4 -> ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorSecondary)
            else -> ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorTertiary)
        }
        spans.add(ForegroundColorSpan(headingColor))
        
        return spans.toTypedArray()
    }
    
    private fun createInlineCodeSpans(): Array<Any> {
        val backgroundColor = if (themeManager.getCurrentTheme() == ThemeManager.THEME_DARK) {
            Color.parseColor("#2B2B2B")
        } else {
            Color.parseColor("#F5F5F5")
        }
        
        val textColor = ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
        
        return arrayOf(
            TypefaceSpan("monospace"),
            BackgroundColorSpan(backgroundColor),
            ForegroundColorSpan(textColor),
            RelativeSizeSpan(0.9f)
        )
    }
    
    private fun createCodeBlockSpans(): Array<Any> {
        val backgroundColor = if (themeManager.getCurrentTheme() == ThemeManager.THEME_DARK) {
            Color.parseColor("#2B2B2B")
        } else {
            Color.parseColor("#F8F8F8")
        }
        
        return arrayOf(
            TypefaceSpan("monospace"),
            BackgroundColorSpan(backgroundColor),
            RelativeSizeSpan(0.85f)
        )
    }

    fun setMarkdownContent(markdownText: String) {
        markwon.setMarkdown(textView, markdownText)
        
        // Add long click listener for code copying functionality
        textView.setOnLongClickListener {
            val selectedText = getSelectedText()
            if (selectedText.isNotEmpty()) {
                copyTextToClipboard(selectedText)
                true
            } else {
                false
            }
        }
    }
    
    private fun getSelectedText(): String {
        return try {
            val selectionStart = textView.selectionStart
            val selectionEnd = textView.selectionEnd
            if (selectionStart >= 0 && selectionEnd > selectionStart) {
                textView.text.substring(selectionStart, selectionEnd)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun copyTextToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("markdown_content", text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
    }

    // Enhanced Light Theme for Syntax Highlighting
    private class EnhancedLightSyntaxTheme(private val context: Context) : Prism4jTheme {
        override fun background(): Int = Color.parseColor("#F8F8F8")
        override fun textColor(): Int = Color.parseColor("#24292E")

        override fun apply(language: String, syntax: Syntax, builder: SpannableStringBuilder, start: Int, end: Int) {
            val spanColor = when (syntax.type()) {
                "keyword", "class-name" -> Color.parseColor("#D73A49") // Red
                "operator", "punctuation" -> Color.parseColor("#24292E") // Dark gray
                "string", "attr-value" -> Color.parseColor("#032F62") // Blue
                "number", "boolean" -> Color.parseColor("#005CC5") // Blue
                "comment" -> Color.parseColor("#6A737D") // Gray
                "function" -> Color.parseColor("#6F42C1") // Purple
                "variable" -> Color.parseColor("#E36209") // Orange
                "tag" -> Color.parseColor("#22863A") // Green
                "attr-name" -> Color.parseColor("#6F42C1") // Purple
                else -> Color.parseColor("#24292E")
            }
            
            builder.setSpan(ForegroundColorSpan(spanColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    // Enhanced Dark Theme for Syntax Highlighting
    private class EnhancedDarkSyntaxTheme(private val context: Context) : Prism4jTheme {
        override fun background(): Int = Color.parseColor("#2B2B2B")
        override fun textColor(): Int = Color.parseColor("#E6E1E5")

        override fun apply(language: String, syntax: Syntax, builder: SpannableStringBuilder, start: Int, end: Int) {
            val spanColor = when (syntax.type()) {
                "keyword", "class-name" -> Color.parseColor("#F97583") // Light red
                "operator", "punctuation" -> Color.parseColor("#E6E1E5") // Light gray
                "string", "attr-value" -> Color.parseColor("#9ECBFF") // Light blue
                "number", "boolean" -> Color.parseColor("#79B8FF") // Blue
                "comment" -> Color.parseColor("#6A737D") // Gray
                "function" -> Color.parseColor("#B392F0") // Light purple
                "variable" -> Color.parseColor("#FFB62C") // Orange
                "tag" -> Color.parseColor("#85E89D") // Green
                "attr-name" -> Color.parseColor("#B392F0") // Light purple
                else -> Color.parseColor("#E6E1E5")
            }
            
            builder.setSpan(ForegroundColorSpan(spanColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    // Material You Dynamic Theme for Syntax Highlighting
    private class MaterialYouSyntaxTheme(private val context: Context) : Prism4jTheme {
        private val colorPrimary = ThemeUtils.getMaterialYouColor(context, 
            com.google.android.material.R.attr.colorPrimary)
        private val colorOnSurface = ThemeUtils.getMaterialYouColor(context, 
            com.google.android.material.R.attr.colorOnSurfaceVariant)
        private val colorSecondary = ThemeUtils.getMaterialYouColor(context, 
            com.google.android.material.R.attr.colorSecondary)
        private val colorTertiary = ThemeUtils.getMaterialYouColor(context, 
            com.google.android.material.R.attr.colorTertiary)
        private val colorError = ThemeUtils.getMaterialYouColor(context, 
            com.google.android.material.R.attr.colorError)

        override fun background(): Int = ThemeUtils.getMaterialYouColor(context, 
            com.google.android.material.R.attr.colorSurfaceVariant)

        override fun textColor(): Int = colorOnSurface

        override fun apply(language: String, syntax: Syntax, builder: SpannableStringBuilder, start: Int, end: Int) {
            val tokenType = syntax.type()
            val spanColor = when (tokenType) {
                "keyword", "class-name" -> colorPrimary
                "operator", "punctuation" -> colorOnSurface
                "string", "attr-value" -> colorSecondary
                "number", "boolean" -> colorTertiary
                "comment" -> ThemeUtils.adjustAlpha(colorOnSurface, 0.6f)
                "function" -> ThemeUtils.adjustBrightness(colorPrimary, 1.2f)
                "variable" -> ThemeUtils.adjustAlpha(colorPrimary, 0.8f)
                "tag" -> colorSecondary
                "attr-name" -> colorTertiary
                "deletion" -> colorError
                "addition" -> colorSecondary
                else -> colorOnSurface
            }
            
            builder.setSpan(ForegroundColorSpan(spanColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

// Bundle all supported languages into a generated GrammarLocator
@PrismBundle(
    includeAll = true,
    grammarLocatorClassName = ".Prism4jGrammarLocatorDef"
)
class Prism4jGrammarBundle
