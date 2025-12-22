package com.kelexine.azubimark.unit

import com.kelexine.azubimark.domain.markdown.InteractiveTaskListHandler
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize as mapShouldHaveSize

/**
 * Unit tests for InteractiveTaskListHandler edge cases.
 * 
 * Tests error conditions, recovery scenarios, and edge cases
 * for task list handling functionality.
 */
class InteractiveTaskListHandlerTest : DescribeSpec({

    describe("InteractiveTaskListHandler") {
        
        describe("empty and null content handling") {
            
            it("should handle empty markdown content") {
                val handler = InteractiveTaskListHandler()
                val result = handler.processTaskList("")
                
                result shouldBe ""
                handler.getTaskCount() shouldBe 0
                handler.getTaskStates() mapShouldHaveSize 0
            }
            
            it("should handle markdown with no task lists") {
                val handler = InteractiveTaskListHandler()
                val markdown = """
                    # Heading
                    
                    Some regular text.
                    
                    - Regular list item
                    - Another item
                """.trimIndent()
                
                val result = handler.processTaskList(markdown)
                
                result shouldBe markdown
                handler.getTaskCount() shouldBe 0
            }
            
            it("should handle whitespace-only content") {
                val handler = InteractiveTaskListHandler()
                val result = handler.processTaskList("   \n\t\n   ")
                
                handler.getTaskCount() shouldBe 0
            }
        }
        
        describe("invalid position handling") {
            
            it("should handle toggle with negative position") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1")
                
                val result = handler.toggleTask(-1, true)
                
                // Should return original markdown unchanged
                handler.isTaskChecked(0) shouldBe false
            }
            
            it("should handle toggle with position beyond task count") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1\n- [ ] Task 2")
                
                val result = handler.toggleTask(10, true)
                
                // Original states should be unchanged
                handler.isTaskChecked(0) shouldBe false
                handler.isTaskChecked(1) shouldBe false
            }
            
            it("should return null for getTaskInfo with invalid position") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1")
                
                handler.getTaskInfo(-1) shouldBe null
                handler.getTaskInfo(5) shouldBe null
            }
        }

        
        describe("task list format variations") {
            
            it("should handle tasks with different bullet markers") {
                val handler = InteractiveTaskListHandler()
                val markdown = """
                    - [ ] Dash task
                    * [ ] Asterisk task
                    + [ ] Plus task
                """.trimIndent()
                
                handler.processTaskList(markdown)
                
                handler.getTaskCount() shouldBe 3
            }
            
            it("should handle mixed checked and unchecked tasks") {
                val handler = InteractiveTaskListHandler()
                val markdown = """
                    - [x] Checked task
                    - [ ] Unchecked task
                    - [X] Uppercase checked
                """.trimIndent()
                
                handler.processTaskList(markdown)
                
                handler.isTaskChecked(0) shouldBe true
                handler.isTaskChecked(1) shouldBe false
                handler.isTaskChecked(2) shouldBe true
            }
            
            it("should handle tasks with leading whitespace (nested)") {
                val handler = InteractiveTaskListHandler()
                val markdown = """
                    - [ ] Parent task
                      - [ ] Nested task
                        - [ ] Deeply nested
                """.trimIndent()
                
                handler.processTaskList(markdown)
                
                handler.getTaskCount() shouldBe 3
            }
            
            it("should handle tasks with special characters in text") {
                val handler = InteractiveTaskListHandler()
                val markdown = "- [ ] Task with `code` and **bold** and [link](url)"
                
                handler.processTaskList(markdown)
                
                val taskInfo = handler.getTaskInfo(0)
                taskInfo shouldNotBe null
                taskInfo?.text shouldBe "Task with `code` and **bold** and [link](url)"
            }
        }
        
        describe("state management") {
            
            it("should preserve original markdown after toggles") {
                val handler = InteractiveTaskListHandler()
                val original = "- [ ] Task 1\n- [ ] Task 2"
                
                handler.processTaskList(original)
                handler.toggleTask(0, true)
                handler.toggleTask(1, true)
                
                handler.getOriginalMarkdown() shouldBe original
            }
            
            it("should reset to original states correctly") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1\n- [x] Task 2")
                
                // Modify states
                handler.toggleTask(0, true)
                handler.toggleTask(1, false)
                
                // Reset
                handler.resetToOriginal()
                
                handler.isTaskChecked(0) shouldBe false
                handler.isTaskChecked(1) shouldBe true
            }
            
            it("should check all tasks correctly") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1\n- [ ] Task 2\n- [ ] Task 3")
                
                handler.checkAllTasks()
                
                handler.getCompletedTaskCount() shouldBe 3
            }
            
            it("should uncheck all tasks correctly") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [x] Task 1\n- [x] Task 2\n- [x] Task 3")
                
                handler.uncheckAllTasks()
                
                handler.getCompletedTaskCount() shouldBe 0
            }
        }
        
        describe("rendered markdown generation") {
            
            it("should generate correct rendered markdown after toggle") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1")
                
                handler.toggleTask(0, true)
                val rendered = handler.getRenderedMarkdown()
                
                rendered shouldBe "- [x] Task 1"
            }
            
            it("should handle multiple toggles correctly") {
                val handler = InteractiveTaskListHandler()
                handler.processTaskList("- [ ] Task 1\n- [ ] Task 2")
                
                handler.toggleTask(0, true)
                handler.toggleTask(1, true)
                handler.toggleTask(0, false)
                
                val rendered = handler.getRenderedMarkdown()
                
                rendered shouldBe "- [ ] Task 1\n- [x] Task 2"
            }
        }
        
        describe("companion object utilities") {
            
            it("should correctly identify task list items") {
                InteractiveTaskListHandler.isTaskListItem("- [ ] Task") shouldBe true
                InteractiveTaskListHandler.isTaskListItem("- [x] Task") shouldBe true
                InteractiveTaskListHandler.isTaskListItem("* [ ] Task") shouldBe true
                InteractiveTaskListHandler.isTaskListItem("+ [X] Task") shouldBe true
                InteractiveTaskListHandler.isTaskListItem("  - [ ] Indented") shouldBe true
                
                InteractiveTaskListHandler.isTaskListItem("- Regular item") shouldBe false
                InteractiveTaskListHandler.isTaskListItem("Not a task") shouldBe false
                InteractiveTaskListHandler.isTaskListItem("") shouldBe false
            }
            
            it("should extract task text correctly") {
                InteractiveTaskListHandler.extractTaskText("- [ ] My task") shouldBe "My task"
                InteractiveTaskListHandler.extractTaskText("- [x] Done task") shouldBe "Done task"
                InteractiveTaskListHandler.extractTaskText("  - [ ] Indented task") shouldBe "Indented task"
                
                InteractiveTaskListHandler.extractTaskText("Not a task") shouldBe null
            }
            
            it("should detect checked state correctly") {
                InteractiveTaskListHandler.isTaskChecked("- [x] Task") shouldBe true
                InteractiveTaskListHandler.isTaskChecked("- [X] Task") shouldBe true
                InteractiveTaskListHandler.isTaskChecked("- [ ] Task") shouldBe false
                InteractiveTaskListHandler.isTaskChecked("Not a task") shouldBe false
            }
        }
    }
})
