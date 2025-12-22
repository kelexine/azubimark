package com.kelexine.azubimark.property

import com.kelexine.azubimark.domain.markdown.InteractiveTaskListHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for task list functionality.
 *
 * Feature: azubimark-android-app
 * Tests Properties 5, 13, and 15 for task list interactivity.
 * Validates: Requirements 1.5, 4.2, 4.3, 4.5
 */
class TaskListPropertiesTest : StringSpec({

    /**
     * Property 5: Task List Interactivity
     *
     * For any Markdown content containing task list syntax, the rendered output
     * should display interactive checkbox elements that can be toggled.
     *
     * Validates: Requirements 1.5
     */
    "Property 5: Task lists are correctly parsed and counted" {
        checkAll(100, Arb.list(Arb.string(1..30), 1..10)) { taskTexts ->
            val markdown = taskTexts.mapIndexed { index, text ->
                val checked = index % 2 == 0
                "- [${if (checked) "x" else " "}] $text"
            }.joinToString("\n")

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Task count should match the number of task items
            handler.getTaskCount() shouldBe taskTexts.size
        }
    }

    "Property 5: Task list items are recognized with different bullet markers" {
        val bulletMarkers = listOf("-", "*", "+")

        checkAll(100, Arb.string(1..30)) { taskText ->
            bulletMarkers.forEach { marker ->
                val uncheckedLine = "$marker [ ] $taskText"
                val checkedLine = "$marker [x] $taskText"

                InteractiveTaskListHandler.isTaskListItem(uncheckedLine) shouldBe true
                InteractiveTaskListHandler.isTaskListItem(checkedLine) shouldBe true
            }
        }
    }

    "Property 5: Non-task list items are not recognized as tasks" {
        val nonTaskLines = listOf(
            "- Regular list item",
            "* Another list item",
            "Normal paragraph text",
            "# Header",
            "```code block```",
            "- [] Missing space",
            "- [xx] Invalid checkbox"
        )

        nonTaskLines.forEach { line ->
            InteractiveTaskListHandler.isTaskListItem(line) shouldBe false
        }
    }

    /**
     * Property 13: Task State Toggle Consistency
     *
     * For any task list item interaction, toggling the checkbox should update
     * both the visual state and internal task state correctly.
     *
     * Validates: Requirements 4.2, 4.3
     */
    "Property 13: Toggling task updates internal state correctly" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..5), Arb.int(0..4)) { taskTexts, toggleIndex ->
            if (toggleIndex < taskTexts.size) {
                val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")

                val handler = InteractiveTaskListHandler()
                handler.processTaskList(markdown)

                // Initially all tasks should be unchecked
                handler.isTaskChecked(toggleIndex) shouldBe false

                // Toggle to checked
                handler.toggleTask(toggleIndex, true)
                handler.isTaskChecked(toggleIndex) shouldBe true

                // Toggle back to unchecked
                handler.toggleTask(toggleIndex, false)
                handler.isTaskChecked(toggleIndex) shouldBe false
            }
        }
    }

    "Property 13: Toggle preserves other task states" {
        checkAll(100, Arb.list(Arb.boolean(), 2..5)) { initialStates ->
            val markdown = initialStates.mapIndexed { index, checked ->
                "- [${if (checked) "x" else " "}] Task $index"
            }.joinToString("\n")

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Toggle the first task
            val newState = !initialStates[0]
            handler.toggleTask(0, newState)

            // First task should have new state
            handler.isTaskChecked(0) shouldBe newState

            // Other tasks should retain their original states
            for (i in 1 until initialStates.size) {
                handler.isTaskChecked(i) shouldBe initialStates[i]
            }
        }
    }

    "Property 13: Task state is reflected in rendered markdown" {
        checkAll(100, Arb.string(1..30)) { taskText ->
            val markdown = "- [ ] $taskText"

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Initially unchecked
            var rendered = handler.getRenderedMarkdown()
            rendered.contains("[ ]") shouldBe true
            rendered.contains("[x]") shouldBe false

            // After toggling to checked
            handler.toggleTask(0, true)
            rendered = handler.getRenderedMarkdown()
            rendered.contains("[x]") shouldBe true
            rendered.contains("[ ]") shouldBe false
        }
    }

    /**
     * Property 15: Source File Preservation
     *
     * For any task list interactions, the original Markdown file content
     * should remain unmodified on the file system.
     *
     * Validates: Requirements 4.5
     */
    "Property 15: Original markdown is never modified" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..5)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Store original
            val original = handler.getOriginalMarkdown()
            original shouldBe markdown

            // Toggle all tasks
            for (i in taskTexts.indices) {
                handler.toggleTask(i, true)
            }

            // Original should still be unchanged
            handler.getOriginalMarkdown() shouldBe markdown
            handler.getOriginalMarkdown() shouldBe original
        }
    }

    "Property 15: Reset restores original state" {
        checkAll(100, Arb.list(Arb.boolean(), 1..5)) { initialStates ->
            val markdown = initialStates.mapIndexed { index, checked ->
                "- [${if (checked) "x" else " "}] Task $index"
            }.joinToString("\n")

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Store initial states
            val originalStates = handler.getTaskStates().toMap()

            // Modify all tasks
            for (i in initialStates.indices) {
                handler.toggleTask(i, !initialStates[i])
            }

            // Reset to original
            handler.resetToOriginal()

            // States should match original
            handler.getTaskStates() shouldBe originalStates
        }
    }

    /**
     * Additional property tests for task list functionality
     */
    "Property: Task text extraction is correct" {
        checkAll(100, Arb.string(1..50)) { taskText ->
            // Trim the task text since extractTaskText trims the result
            val trimmedText = taskText.trim()
            if (trimmedText.isNotEmpty()) {
                val line = "- [ ] $trimmedText"
                val extracted = InteractiveTaskListHandler.extractTaskText(line)
                extracted shouldBe trimmedText
            }
        }
    }

    "Property: Checked state detection is correct" {
        checkAll(100, Arb.string(1..30)) { taskText ->
            val uncheckedLine = "- [ ] $taskText"
            val checkedLine = "- [x] $taskText"
            val checkedUpperLine = "- [X] $taskText"

            InteractiveTaskListHandler.isTaskChecked(uncheckedLine) shouldBe false
            InteractiveTaskListHandler.isTaskChecked(checkedLine) shouldBe true
            InteractiveTaskListHandler.isTaskChecked(checkedUpperLine) shouldBe true
        }
    }

    "Property: Task count matches completed count when all checked" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..10)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Check all tasks
            handler.checkAllTasks()

            handler.getCompletedTaskCount() shouldBe handler.getTaskCount()
        }
    }

    "Property: Completed count is zero when all unchecked" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..10)) { taskTexts ->
            val markdown = taskTexts.map { "- [x] $it" }.joinToString("\n")

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Uncheck all tasks
            handler.uncheckAllTasks()

            handler.getCompletedTaskCount() shouldBe 0
        }
    }

    "Property: getAllTasks returns correct task info" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..5)) { taskTexts ->
            // Trim task texts since the handler trims them
            val trimmedTexts = taskTexts.map { it.trim() }.filter { it.isNotEmpty() }
            if (trimmedTexts.isNotEmpty()) {
                val markdown = trimmedTexts.mapIndexed { index, text ->
                    "- [${if (index % 2 == 0) "x" else " "}] $text"
                }.joinToString("\n")

                val handler = InteractiveTaskListHandler()
                handler.processTaskList(markdown)

                val tasks = handler.getAllTasks()

                tasks.size shouldBe trimmedTexts.size
                tasks.forEachIndexed { index, taskInfo ->
                    taskInfo.position shouldBe index
                    taskInfo.text shouldBe trimmedTexts[index]
                    taskInfo.isChecked shouldBe (index % 2 == 0)
                }
            }
        }
    }

    "Property: Invalid position toggle returns original markdown" {
        checkAll(100, Arb.string(1..30)) { taskText ->
            val markdown = "- [ ] $taskText"

            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            // Toggle with invalid positions
            val result1 = handler.toggleTask(-1, true)
            val result2 = handler.toggleTask(100, true)

            result1 shouldBe markdown
            result2 shouldBe markdown
        }
    }

    "Property: Empty markdown has zero tasks" {
        val emptyMarkdowns = listOf("", "No tasks here", "# Just a header", "- Regular list item")

        emptyMarkdowns.forEach { markdown ->
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)

            handler.getTaskCount() shouldBe 0
            handler.getCompletedTaskCount() shouldBe 0
        }
    }
})
