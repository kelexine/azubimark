package me.kelexine.azubimark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.noties.prism4j.Prism4j.Syntax
import io.noties.prism4j.annotations.PrismBundle

// Generated into the same package by prism4j-bundler
import me.kelexine.azubimark.Prism4jGrammarLocatorDef

class MarkdownViewer(
    private val context: Context,
    private val textView: TextView
) {
    private val themeManager = ThemeManager(context)
    private val prism4j = Prism4j(Prism4jGrammarLocatorDef())

    private val markwon = Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(createSyntaxHighlightPlugin())
        .usePlugin(createCodeBlockPlugin())
        .build()

    private fun createSyntaxHighlightPlugin(): SyntaxHighlightPlugin =
        SyntaxHighlightPlugin.create(
            prism4j,
            when (themeManager.getCurrentTheme()) {
                ThemeManager.THEME_LIGHT -> Prism4jThemeDefault.create()
                ThemeManager.THEME_DARK  -> Prism4jThemeDarkula.create()
                else                    -> CustomMaterialYouTheme(context)
            }
        )

    private fun createCodeBlockPlugin(): AbstractMarkwonPlugin =
        object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, _ -> /* handle links if needed */ }
            }
        }

    fun setMarkdownContent(markdownText: String) {
        markwon.setMarkdown(textView, markdownText)
    }

    private fun addCopyButtonToCodeBlock(codeBlockView: View, codeText: String) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("code", codeText)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private class CustomMaterialYouTheme(context: Context) : Prism4jTheme {
        private val colorPrimary =
            ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorPrimary)
        private val colorOnSurface =
            ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorOnSurface)
        private val colorSurfaceVariant =
            ThemeUtils.getMaterialYouColor(context, com.google.android.material.R.attr.colorSurfaceVariant)

        override fun background(): Int = colorSurfaceVariant

        override fun textColor(): Int = colorOnSurface

        override fun apply(
            language: String,
            syntax: Syntax,
            builder: SpannableStringBuilder,
            start: Int,
            end: Int
        ) {
            // use syntax.type() to get the token name 1
            val tokenType = syntax.type()
            val spanColor = when (tokenType) {
                "keyword"      -> colorPrimary
                "operator",
                "punctuation"  -> colorOnSurface
                "string"       -> ThemeUtils.adjustAlpha(colorPrimary, 0.8f)
                "number"       -> ThemeUtils.adjustAlpha(colorPrimary, 0.7f)
                "comment"      -> ThemeUtils.adjustAlpha(colorOnSurface, 0.6f)
                "function"     -> ThemeUtils.adjustBrightness(colorPrimary, 1.2f)
                "class-name"   -> ThemeUtils.adjustBrightness(colorPrimary, 1.3f)
                else           -> colorOnSurface
            }
            builder.setSpan(
                ForegroundColorSpan(spanColor),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}

// Bundle all supported languages into a generated GrammarLocator
@PrismBundle(
    includeAll = true,
    grammarLocatorClassName = ".Prism4jGrammarLocatorDef"
)
class Prism4jGrammarBundle
