package me.kelexine.azubimark

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SearchManager(
    private val context: Context,
    private val contentView: TextView,
    private val onSearchResult: (String) -> Unit
) {
    
    private var currentContent: String = ""
    private var searchDialog: AlertDialog? = null
    private var currentSearchQuery: String = ""
    
    fun updateContent(content: String) {
        currentContent = content
        currentSearchQuery = ""
    }
    
    fun showSearchDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_search, null)
        val searchInputLayout = dialogView.findViewById<TextInputLayout>(R.id.search_input_layout)
        val searchEditText = dialogView.findViewById<TextInputEditText>(R.id.search_edit_text)
        
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty() && query != currentSearchQuery) {
                    currentSearchQuery = query
                    performSearch(query)
                }
            }
        })
        
        searchDialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.search_document))
            .setView(dialogView)
            .setPositiveButton(context.getString(android.R.string.ok)) { _, _ ->
                val query = searchEditText.text?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    onSearchResult(query)
                }
            }
            .setNegativeButton(context.getString(android.R.string.cancel), null)
            .setNeutralButton(context.getString(R.string.clear_search)) { _, _ ->
                clearSearch()
            }
            .create()
        
        searchDialog?.show()
        
        // Focus the search field
        searchEditText.requestFocus()
    }
    
    private fun performSearch(query: String) {
        if (query.isEmpty() || currentContent.isEmpty()) return
        
        // Count occurrences
        val occurrences = countOccurrences(currentContent, query)
        
        // Update search input layout helper text
        searchDialog?.findViewById<TextInputLayout>(R.id.search_input_layout)?.let { layout ->
            layout.helperText = if (occurrences > 0) {
                context.getString(R.string.search_results_count, occurrences)
            } else {
                context.getString(R.string.no_search_results)
            }
        }
    }
    
    private fun countOccurrences(text: String, query: String): Int {
        if (query.isEmpty()) return 0
        
        var count = 0
        var index = 0
        
        while (true) {
            index = text.indexOf(query, index, ignoreCase = true)
            if (index == -1) break
            count++
            index += query.length
        }
        
        return count
    }
    
    private fun clearSearch() {
        currentSearchQuery = ""
        // You could implement clearing of any search highlights here
    }
    
    fun searchNext(query: String): Boolean {
        if (query.isEmpty() || currentContent.isEmpty()) return false
        
        val currentIndex = getCurrentSearchIndex()
        val nextIndex = currentContent.indexOf(query, currentIndex + 1, ignoreCase = true)
        
        return if (nextIndex >= 0) {
            onSearchResult(query)
            true
        } else {
            // Wrap around to beginning
            val firstIndex = currentContent.indexOf(query, 0, ignoreCase = true)
            if (firstIndex >= 0) {
                onSearchResult(query)
                true
            } else {
                false
            }
        }
    }
    
    fun searchPrevious(query: String): Boolean {
        if (query.isEmpty() || currentContent.isEmpty()) return false
        
        val currentIndex = getCurrentSearchIndex()
        val previousIndex = currentContent.lastIndexOf(query, currentIndex - 1, ignoreCase = true)
        
        return if (previousIndex >= 0) {
            onSearchResult(query)
            true
        } else {
            // Wrap around to end
            val lastIndex = currentContent.lastIndexOf(query, ignoreCase = true)
            if (lastIndex >= 0) {
                onSearchResult(query)
                true
            } else {
                false
            }
        }
    }
    
    private fun getCurrentSearchIndex(): Int {
        // This is a simplified implementation
        // In a real app, you'd track the current search position
        return 0
    }
    
    fun getCurrentQuery(): String = currentSearchQuery
    
    fun hasContent(): Boolean = currentContent.isNotEmpty()
}