package com.kelexine.azubimark.domain.markdown

/**
 * Interface for handling interactive task lists in Markdown.
 * Manages task state without modifying source files.
 */
interface TaskListHandler {
    /**
     * Process task list markdown and prepare for rendering.
     */
    fun processTaskList(markdown: String): String

    /**
     * Toggle a task's checked state at the given position.
     */
    fun toggleTask(position: Int, isChecked: Boolean): String

    /**
     * Get current task states.
     */
    fun getTaskStates(): Map<Int, Boolean>
}
