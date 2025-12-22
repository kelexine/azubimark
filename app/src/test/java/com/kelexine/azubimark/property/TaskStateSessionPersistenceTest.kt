package com.kelexine.azubimark.property

import com.kelexine.azubimark.domain.markdown.InteractiveTaskListHandler
import com.kelexine.azubimark.ui.viewer.FileState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for task state session persistence.
 *
 * Feature: azubimark-android-app, Property 14: Task State Session Persistence
 * 
 * Tests that task state modifications persist during the current session.
 * 
 * Validates: Requirements 4.4
 */
class TaskStateSessionPersistenceTest : StringSpec({

    /**
     * Property 14: Task State Session Persistence
     *
     * For any task state modifications during a session, the changes should be
     * maintained until the file is closed or the application is terminated.
     *
     * Validates: Requirements 4.4
     */
    "Property 14: Task state modifications persist within session" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..10)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")
            
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)
            
            // Modify some tasks
            val modifications = mutableMapOf<Int, Boolean>()
            taskTexts.indices.forEach { index ->
                val newState = index % 2 == 0
                handler.toggleTask(index, newState)
                modifications[index] = newState
            }
            
            // Verify all modifications persist
            modifications.forEach { (position, expectedState) ->
                handler.isTaskChecked(position) shouldBe expectedState
            }
            
            // Verify states are accessible via getTaskStates
            val states = handler.getTaskStates()
            modifications.forEach { (position, expectedState) ->
                states[position] shouldBe expectedState
            }
        }
    }

    "Property 14: Multiple sequential toggles maintain final state" {
        checkAll(100, Arb.string(1..30), Arb.list(Arb.boolean(), 2..10)) { taskText, toggleSequence ->
            val markdown = "- [ ] $taskText"
            
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)
            
            // Apply sequence of toggles
            var expectedFinalState = false
            toggleSequence.forEach { newState ->
                handler.toggleTask(0, newState)
                expectedFinalState = newState
            }
            
            // Final state should match last toggle
            handler.isTaskChecked(0) shouldBe expectedFinalState
            handler.getTaskStates()[0] shouldBe expectedFinalState
        }
    }

    "Property 14: Task states persist across multiple task interactions" {
        checkAll(100, Arb.list(Arb.boolean(), 3..8)) { initialStates ->
            val markdown = initialStates.mapIndexed { index, checked ->
                "- [${if (checked) "x" else " "}] Task $index"
            }.joinToString("\n")
            
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)
            
            // Toggle each task multiple times
            val finalStates = mutableMapOf<Int, Boolean>()
            initialStates.indices.forEach { index ->
                // Toggle twice - should end up at opposite of initial
                handler.toggleTask(index, !initialStates[index])
                handler.toggleTask(index, initialStates[index])
                handler.toggleTask(index, !initialStates[index])
                finalStates[index] = !initialStates[index]
            }
            
            // Verify all final states
            finalStates.forEach { (position, expectedState) ->
                handler.isTaskChecked(position) shouldBe expectedState
            }
        }
    }

    "Property 14: Task states are independent - modifying one doesn't affect others" {
        checkAll(100, Arb.list(Arb.string(1..15), 3..6), Arb.int(0..5)) { taskTexts, targetIndex ->
            if (targetIndex < taskTexts.size) {
                val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")
                
                val handler = InteractiveTaskListHandler()
                handler.processTaskList(markdown)
                
                // Store initial states (all unchecked)
                val initialStates = handler.getTaskStates().toMap()
                
                // Toggle only the target task
                handler.toggleTask(targetIndex, true)
                
                // Verify only target changed
                taskTexts.indices.forEach { index ->
                    val expectedState = if (index == targetIndex) true else initialStates[index]
                    handler.isTaskChecked(index) shouldBe expectedState
                }
            }
        }
    }

    "Property 14: FileState correctly stores task states" {
        checkAll(100, Arb.list(Arb.boolean(), 1..5)) { taskStates ->
            val statesMap = taskStates.mapIndexed { index, checked -> index to checked }.toMap()
            
            val fileState = FileState(
                scrollPosition = 100,
                taskStates = statesMap
            )
            
            // Verify FileState preserves all task states
            fileState.taskStates.size shouldBe taskStates.size
            statesMap.forEach { (position, expectedState) ->
                fileState.taskStates[position] shouldBe expectedState
            }
        }
    }

    "Property 14: Task states map preserves all entries correctly" {
        checkAll(100, Arb.list(Arb.boolean(), 1..10)) { taskStates ->
            val statesMap = taskStates.mapIndexed { index, checked -> index to checked }.toMap()
            
            // Verify map preserves all task states correctly
            statesMap.size shouldBe taskStates.size
            taskStates.forEachIndexed { index, expectedState ->
                statesMap[index] shouldBe expectedState
            }
            
            // Verify map can be copied and still preserves states
            val copiedMap = statesMap.toMap()
            copiedMap.size shouldBe statesMap.size
            statesMap.forEach { (position, expectedState) ->
                copiedMap[position] shouldBe expectedState
            }
        }
    }

    "Property 14: Rendered markdown reflects current task states" {
        checkAll(100, Arb.list(Arb.string(1..15), 2..5)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")
            
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)
            
            // Check some tasks
            val checkedIndices = taskTexts.indices.filter { it % 2 == 0 }
            checkedIndices.forEach { index ->
                handler.toggleTask(index, true)
            }
            
            // Verify rendered markdown reflects states
            val rendered = handler.getRenderedMarkdown()
            val lines = rendered.split("\n")
            
            taskTexts.indices.forEach { index ->
                val expectedChecked = index in checkedIndices
                val line = lines[index]
                if (expectedChecked) {
                    line.contains("[x]") shouldBe true
                } else {
                    line.contains("[ ]") shouldBe true
                }
            }
        }
    }

    "Property 14: Session state survives content re-parsing" {
        checkAll(100, Arb.list(Arb.string(1..15), 2..4)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")
            
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)
            
            // Modify states
            taskTexts.indices.forEach { index ->
                handler.toggleTask(index, true)
            }
            
            // Get current states
            val statesBeforeReparse = handler.getTaskStates().toMap()
            
            // Simulate re-parsing by getting rendered markdown and processing again
            val renderedMarkdown = handler.getRenderedMarkdown()
            val newHandler = InteractiveTaskListHandler()
            newHandler.processTaskList(renderedMarkdown)
            
            // States should be preserved in the rendered content
            val statesAfterReparse = newHandler.getTaskStates()
            statesBeforeReparse.forEach { (position, expectedState) ->
                statesAfterReparse[position] shouldBe expectedState
            }
        }
    }
})
