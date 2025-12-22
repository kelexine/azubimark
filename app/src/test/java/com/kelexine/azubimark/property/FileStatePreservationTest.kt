package com.kelexine.azubimark.property

import com.kelexine.azubimark.domain.markdown.InteractiveTaskListHandler
import com.kelexine.azubimark.ui.viewer.FileState
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
 * Property-based tests for file state preservation.
 *
 * Feature: azubimark-android-app, Property 17: File State Preservation
 * 
 * Tests that scroll position and task states are preserved when switching files.
 * 
 * Validates: Requirements 6.4
 */
class FileStatePreservationTest : StringSpec({

    /**
     * Property 17: File State Preservation
     *
     * For any file switching operation, the previous file's scroll position
     * and task states should be preserved when returning to that file.
     *
     * Validates: Requirements 6.4
     */
    "Property 17: FileState preserves scroll position correctly" {
        checkAll(100, Arb.int(0..10000)) { scrollPosition ->
            val fileState = FileState(
                scrollPosition = scrollPosition,
                taskStates = emptyMap()
            )
            
            fileState.scrollPosition shouldBe scrollPosition
        }
    }

    "Property 17: FileState preserves task states correctly" {
        checkAll(100, Arb.list(Arb.boolean(), 1..10)) { taskStates ->
            val statesMap = taskStates.mapIndexed { index, checked -> index to checked }.toMap()
            
            val fileState = FileState(
                scrollPosition = 0,
                taskStates = statesMap
            )
            
            fileState.taskStates.size shouldBe taskStates.size
            statesMap.forEach { (position, expectedState) ->
                fileState.taskStates[position] shouldBe expectedState
            }
        }
    }

    "Property 17: FileState preserves both scroll position and task states" {
        checkAll(100, Arb.int(0..5000), Arb.list(Arb.boolean(), 1..8)) { scrollPosition, taskStates ->
            val statesMap = taskStates.mapIndexed { index, checked -> index to checked }.toMap()
            
            val fileState = FileState(
                scrollPosition = scrollPosition,
                taskStates = statesMap
            )
            
            fileState.scrollPosition shouldBe scrollPosition
            fileState.taskStates.size shouldBe taskStates.size
            statesMap.forEach { (position, expectedState) ->
                fileState.taskStates[position] shouldBe expectedState
            }
        }
    }

    "Property 17: File state cache simulation - states persist across file switches" {
        checkAll(100, Arb.list(Arb.string(5..15), 2..5)) { fileNames ->
            // Simulate a file state cache
            val fileStateCache = mutableMapOf<String, FileState>()
            
            // Create states for each file
            val expectedStates = fileNames.mapIndexed { index, fileName ->
                val scrollPos = index * 100
                val taskStates = (0..index).associate { it to (it % 2 == 0) }
                fileName to FileState(scrollPos, taskStates)
            }.toMap()
            
            // Store states in cache
            expectedStates.forEach { (fileName, state) ->
                fileStateCache[fileName] = state
            }
            
            // Verify all states are preserved
            expectedStates.forEach { (fileName, expectedState) ->
                val cachedState = fileStateCache[fileName]
                cachedState shouldNotBe null
                cachedState!!.scrollPosition shouldBe expectedState.scrollPosition
                cachedState.taskStates shouldBe expectedState.taskStates
            }
        }
    }

    "Property 17: Task handler state can be restored from FileState" {
        checkAll(100, Arb.list(Arb.string(1..15), 2..5)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")
            
            // Create initial handler and modify states
            val handler1 = InteractiveTaskListHandler()
            handler1.processTaskList(markdown)
            
            // Toggle some tasks
            val modifications = taskTexts.indices.associate { it to (it % 2 == 0) }
            modifications.forEach { (position, isChecked) ->
                handler1.toggleTask(position, isChecked)
            }
            
            // Save state to FileState
            val savedState = FileState(
                scrollPosition = 500,
                taskStates = handler1.getTaskStates()
            )
            
            // Create new handler (simulating file reload)
            val handler2 = InteractiveTaskListHandler()
            handler2.processTaskList(markdown)
            
            // Restore states from FileState
            savedState.taskStates.forEach { (position, isChecked) ->
                handler2.toggleTask(position, isChecked)
            }
            
            // Verify states match
            handler2.getTaskStates() shouldBe savedState.taskStates
        }
    }

    "Property 17: Scroll position updates are preserved" {
        checkAll(100, Arb.list(Arb.int(0..5000), 2..10)) { scrollPositions ->
            var currentState = FileState(scrollPosition = 0, taskStates = emptyMap())
            
            // Simulate scroll position updates
            scrollPositions.forEach { newPosition ->
                currentState = currentState.copy(scrollPosition = newPosition)
            }
            
            // Final state should have the last scroll position
            currentState.scrollPosition shouldBe scrollPositions.last()
        }
    }

    "Property 17: Task state updates are preserved across multiple modifications" {
        checkAll(100, Arb.list(Arb.string(1..10), 3..6)) { taskTexts ->
            val markdown = taskTexts.map { "- [ ] $it" }.joinToString("\n")
            
            val handler = InteractiveTaskListHandler()
            handler.processTaskList(markdown)
            
            // Perform multiple modifications
            val finalStates = mutableMapOf<Int, Boolean>()
            taskTexts.indices.forEach { index ->
                // Toggle multiple times
                handler.toggleTask(index, true)
                handler.toggleTask(index, false)
                handler.toggleTask(index, true)
                finalStates[index] = true
            }
            
            // Save to FileState
            val fileState = FileState(
                scrollPosition = 1000,
                taskStates = handler.getTaskStates()
            )
            
            // Verify final states are preserved
            fileState.taskStates shouldBe finalStates
        }
    }

    "Property 17: Empty file state is valid" {
        val emptyState = FileState()
        
        emptyState.scrollPosition shouldBe 0
        emptyState.taskStates shouldBe emptyMap()
    }

    "Property 17: File state with only scroll position is valid" {
        checkAll(100, Arb.int(0..10000)) { scrollPosition ->
            val state = FileState(scrollPosition = scrollPosition)
            
            state.scrollPosition shouldBe scrollPosition
            state.taskStates shouldBe emptyMap()
        }
    }

    "Property 17: File state with only task states is valid" {
        checkAll(100, Arb.list(Arb.boolean(), 1..5)) { taskStates ->
            val statesMap = taskStates.mapIndexed { index, checked -> index to checked }.toMap()
            val state = FileState(taskStates = statesMap)
            
            state.scrollPosition shouldBe 0
            state.taskStates shouldBe statesMap
        }
    }

    "Property 17: File state cache handles file removal correctly" {
        checkAll(100, Arb.list(Arb.string(5..15), 3..6)) { fileNames ->
            // Use distinct file names to avoid duplicates
            val distinctFileNames = fileNames.mapIndexed { index, name -> "${name}_$index" }
            val fileStateCache = mutableMapOf<String, FileState>()
            
            // Add states for all files
            distinctFileNames.forEachIndexed { index, fileName ->
                fileStateCache[fileName] = FileState(scrollPosition = index * 100)
            }
            
            // Remove first file
            val removedFile = distinctFileNames.first()
            fileStateCache.remove(removedFile)
            
            // Verify removed file is gone
            fileStateCache.containsKey(removedFile) shouldBe false
            
            // Verify other files still exist
            distinctFileNames.drop(1).forEach { fileName ->
                fileStateCache.containsKey(fileName) shouldBe true
            }
        }
    }

    "Property 17: File state cache handles clear correctly" {
        checkAll(100, Arb.list(Arb.string(5..15), 2..5)) { fileNames ->
            val fileStateCache = mutableMapOf<String, FileState>()
            
            // Add states for all files
            fileNames.forEach { fileName ->
                fileStateCache[fileName] = FileState(scrollPosition = 100)
            }
            
            // Clear cache
            fileStateCache.clear()
            
            // Verify all files are gone
            fileStateCache.isEmpty() shouldBe true
            fileNames.forEach { fileName ->
                fileStateCache.containsKey(fileName) shouldBe false
            }
        }
    }
})
