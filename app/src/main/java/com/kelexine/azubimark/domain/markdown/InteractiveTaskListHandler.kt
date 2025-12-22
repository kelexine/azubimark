package com.kelexine.azubimark.domain.markdown

/**
 * Implementation of TaskListHandler that manages interactive task list state.
 * 
 * Maintains task state in memory without modifying the original source file.
 * Task states are tracked by position and can be toggled independently.
 * 
 * Validates: Requirements 1.5, 4.1, 4.2, 4.3, 4.5
 */
class InteractiveTaskListHandler : TaskListHandler {

    /**
     * Map of task positions to their checked state.
     * Key: position (0-indexed) of the task in the document
     * Value: true if checked, false if unchecked
     */
    private val taskStates: MutableMap<Int, Boolean> = mutableMapOf()

    /**
     * The original markdown content (preserved, never modified).
     */
    private var originalMarkdown: String = ""

    /**
     * Regex pattern to match task list items.
     * Matches: - [ ] or - [x] or - [X] at the start of a line (with optional leading whitespace)
     */
    private val taskPattern = Regex("""^(\s*[-*+]\s*)\[([xX ])\](.*)$""", RegexOption.MULTILINE)

    /**
     * Process task list markdown and initialize task states.
     * 
     * Parses the markdown to find all task list items and initializes
     * their states based on the original content.
     * 
     * @param markdown The original markdown content
     * @return The processed markdown (unchanged, as we don't modify source)
     */
    override fun processTaskList(markdown: String): String {
        originalMarkdown = markdown
        taskStates.clear()

        // Find all task items and initialize their states
        var position = 0
        taskPattern.findAll(markdown).forEach { match ->
            val checkMark = match.groupValues[2]
            taskStates[position] = checkMark.equals("x", ignoreCase = true)
            position++
        }

        return markdown
    }

    /**
     * Toggle a task's checked state at the given position.
     * 
     * Updates the internal state map without modifying the original markdown.
     * Returns the markdown with the visual state updated for rendering.
     * 
     * @param position The 0-indexed position of the task to toggle
     * @param isChecked The new checked state
     * @return The markdown with updated visual state for rendering
     */
    override fun toggleTask(position: Int, isChecked: Boolean): String {
        if (position < 0 || position >= taskStates.size) {
            return originalMarkdown
        }

        taskStates[position] = isChecked
        return getRenderedMarkdown()
    }

    /**
     * Get current task states.
     * 
     * @return An immutable copy of the task states map
     */
    override fun getTaskStates(): Map<Int, Boolean> {
        return taskStates.toMap()
    }

    /**
     * Get the markdown with current task states applied for rendering.
     * 
     * This creates a modified version of the markdown for display purposes only.
     * The original markdown is never modified.
     * 
     * @return Markdown with current task states applied
     */
    fun getRenderedMarkdown(): String {
        if (taskStates.isEmpty()) {
            return originalMarkdown
        }

        var position = 0
        return taskPattern.replace(originalMarkdown) { match ->
            val prefix = match.groupValues[1]
            val suffix = match.groupValues[3]
            val isChecked = taskStates[position] ?: false
            position++
            
            val checkMark = if (isChecked) "x" else " "
            "$prefix[$checkMark]$suffix"
        }
    }

    /**
     * Get the original unmodified markdown.
     * 
     * @return The original markdown content
     */
    fun getOriginalMarkdown(): String = originalMarkdown

    /**
     * Get the total number of tasks in the document.
     * 
     * @return The count of task list items
     */
    fun getTaskCount(): Int = taskStates.size

    /**
     * Get the number of completed tasks.
     * 
     * @return The count of checked task items
     */
    fun getCompletedTaskCount(): Int = taskStates.values.count { it }

    /**
     * Check if a specific task is checked.
     * 
     * @param position The 0-indexed position of the task
     * @return true if checked, false if unchecked or position is invalid
     */
    fun isTaskChecked(position: Int): Boolean = taskStates[position] ?: false

    /**
     * Reset all task states to their original values from the markdown.
     */
    fun resetToOriginal() {
        processTaskList(originalMarkdown)
    }

    /**
     * Check all tasks.
     */
    fun checkAllTasks() {
        taskStates.keys.forEach { position ->
            taskStates[position] = true
        }
    }

    /**
     * Uncheck all tasks.
     */
    fun uncheckAllTasks() {
        taskStates.keys.forEach { position ->
            taskStates[position] = false
        }
    }

    /**
     * Get task information at a specific position.
     * 
     * @param position The 0-indexed position of the task
     * @return TaskInfo containing the task text and state, or null if position is invalid
     */
    fun getTaskInfo(position: Int): TaskInfo? {
        if (position < 0 || position >= taskStates.size) {
            return null
        }

        var currentPosition = 0
        taskPattern.findAll(originalMarkdown).forEach { match ->
            if (currentPosition == position) {
                val text = match.groupValues[3].trim()
                val isChecked = taskStates[position] ?: false
                return TaskInfo(position, text, isChecked)
            }
            currentPosition++
        }
        return null
    }

    /**
     * Get all tasks with their information.
     * 
     * @return List of TaskInfo for all tasks in the document
     */
    fun getAllTasks(): List<TaskInfo> {
        val tasks = mutableListOf<TaskInfo>()
        var position = 0
        
        taskPattern.findAll(originalMarkdown).forEach { match ->
            val text = match.groupValues[3].trim()
            val isChecked = taskStates[position] ?: false
            tasks.add(TaskInfo(position, text, isChecked))
            position++
        }
        
        return tasks
    }

    /**
     * Data class representing task information.
     */
    data class TaskInfo(
        val position: Int,
        val text: String,
        val isChecked: Boolean
    )

    companion object {
        /**
         * Check if a line is a task list item.
         * 
         * @param line The line to check
         * @return true if the line is a task list item
         */
        fun isTaskListItem(line: String): Boolean {
            return line.matches(Regex("""^\s*[-*+]\s*\[[xX ]\].*$"""))
        }

        /**
         * Extract the task text from a task list item line.
         * 
         * @param line The task list item line
         * @return The task text, or null if not a valid task item
         */
        fun extractTaskText(line: String): String? {
            val match = Regex("""^\s*[-*+]\s*\[[xX ]\]\s*(.*)$""").find(line)
            return match?.groupValues?.get(1)?.trim()
        }

        /**
         * Check if a task list item is checked.
         * 
         * @param line The task list item line
         * @return true if checked, false if unchecked or not a valid task item
         */
        fun isTaskChecked(line: String): Boolean {
            val match = Regex("""^\s*[-*+]\s*\[([xX ])\].*$""").find(line)
            return match?.groupValues?.get(1)?.equals("x", ignoreCase = true) ?: false
        }
    }
}
