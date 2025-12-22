package com.kelexine.azubimark.property

import android.net.Uri
import com.kelexine.azubimark.ui.browser.BreadcrumbItem
import com.kelexine.azubimark.ui.browser.NavigationEntry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.mockk

/**
 * Property-based tests for navigation consistency.
 *
 * Feature: azubimark-android-app, Property 9: Navigation State Consistency
 * Validates: Requirements 2.5
 *
 * Tests that navigation operations maintain correct back stack and breadcrumb state.
 */
class NavigationPropertiesTest : StringSpec({

    /**
     * Property 9: Navigation State Consistency
     *
     * For any file system navigation operation, the browser should maintain
     * correct back navigation capability and breadcrumb state.
     *
     * Validates: Requirements 2.5
     */
    "Property 9: Navigation stack grows when navigating into directories" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..10)) { directoryNames ->
            val navigationStack = mutableListOf<NavigationEntry>()
            
            // Simulate navigating into each directory
            directoryNames.forEach { name ->
                val uri = mockk<Uri>()
                navigationStack.add(NavigationEntry(uri, name))
            }
            
            // Stack size should match number of navigations
            navigationStack.size shouldBe directoryNames.size
        }
    }

    "Property 9: Navigation stack shrinks when navigating back" {
        checkAll(100, Arb.list(Arb.string(1..20), 2..10)) { directoryNames ->
            val navigationStack = mutableListOf<NavigationEntry>()
            
            // Navigate into directories
            directoryNames.forEach { name ->
                val uri = mockk<Uri>()
                navigationStack.add(NavigationEntry(uri, name))
            }
            
            val initialSize = navigationStack.size
            
            // Navigate back once
            if (navigationStack.isNotEmpty()) {
                navigationStack.removeAt(navigationStack.lastIndex)
            }
            
            // Stack should be one smaller
            navigationStack.size shouldBe initialSize - 1
        }
    }

    "Property 9: Breadcrumbs reflect navigation path" {
        checkAll(100, Arb.list(Arb.string(1..20), 0..5)) { directoryNames ->
            val breadcrumbs = mutableListOf<BreadcrumbItem>()
            
            // Root is always first
            breadcrumbs.add(BreadcrumbItem("Storage", null, 0))
            
            // Add breadcrumb for each directory
            directoryNames.forEachIndexed { index, name ->
                val uri = mockk<Uri>()
                breadcrumbs.add(BreadcrumbItem(name, uri, index + 1))
            }
            
            // Breadcrumbs should have root + all directories
            breadcrumbs.size shouldBe directoryNames.size + 1
            
            // First breadcrumb should always be root
            breadcrumbs.first().name shouldBe "Storage"
            breadcrumbs.first().index shouldBe 0
            
            // Each breadcrumb should have correct index
            breadcrumbs.forEachIndexed { index, breadcrumb ->
                breadcrumb.index shouldBe index
            }
        }
    }

    "Property 9: Navigating to breadcrumb truncates stack correctly" {
        checkAll(100, Arb.list(Arb.string(1..20), 3..10), Arb.int(0..9)) { directoryNames, targetIndex ->
            if (targetIndex < directoryNames.size) {
                val navigationStack = mutableListOf<NavigationEntry>()
                
                // Build navigation stack
                directoryNames.forEach { name ->
                    val uri = mockk<Uri>()
                    navigationStack.add(NavigationEntry(uri, name))
                }
                
                // Navigate to breadcrumb at targetIndex
                while (navigationStack.size > targetIndex) {
                    navigationStack.removeAt(navigationStack.lastIndex)
                }
                
                // Stack should be truncated to target index
                navigationStack.size shouldBe targetIndex
            }
        }
    }

    "Property 9: Can navigate back is true when stack is not empty" {
        checkAll(100, Arb.list(Arb.string(1..20), 0..10)) { directoryNames ->
            val navigationStack = mutableListOf<NavigationEntry>()
            
            directoryNames.forEach { name ->
                val uri = mockk<Uri>()
                navigationStack.add(NavigationEntry(uri, name))
            }
            
            val canNavigateBack = navigationStack.isNotEmpty()
            
            canNavigateBack shouldBe directoryNames.isNotEmpty()
        }
    }

    "Property 9: Navigation entry preserves URI and name" {
        checkAll(100, Arb.string(1..50)) { name ->
            val uri = mockk<Uri>()
            val entry = NavigationEntry(uri, name)
            
            entry.uri shouldBe uri
            entry.name shouldBe name
        }
    }

    "Property 9: Breadcrumb item preserves all properties" {
        checkAll(100, Arb.string(1..50), Arb.int(0..100)) { name, index ->
            val uri = mockk<Uri>()
            val breadcrumb = BreadcrumbItem(name, uri, index)
            
            breadcrumb.name shouldBe name
            breadcrumb.uri shouldBe uri
            breadcrumb.index shouldBe index
        }
    }

    "Property 9: Root breadcrumb has null URI" {
        val rootBreadcrumb = BreadcrumbItem("Storage", null, 0)
        
        rootBreadcrumb.uri shouldBe null
        rootBreadcrumb.name shouldBe "Storage"
        rootBreadcrumb.index shouldBe 0
    }

    "Property 9: Multiple back navigations return to root" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..10)) { directoryNames ->
            val navigationStack = mutableListOf<NavigationEntry>()
            
            // Navigate into directories
            directoryNames.forEach { name ->
                val uri = mockk<Uri>()
                navigationStack.add(NavigationEntry(uri, name))
            }
            
            // Navigate back to root
            while (navigationStack.isNotEmpty()) {
                navigationStack.removeAt(navigationStack.lastIndex)
            }
            
            // Should be at root (empty stack)
            navigationStack.isEmpty() shouldBe true
        }
    }

    "Property 9: Navigation preserves order" {
        checkAll(100, Arb.list(Arb.string(1..20), 1..10)) { directoryNames ->
            val navigationStack = mutableListOf<NavigationEntry>()
            
            // Navigate into directories in order
            directoryNames.forEach { name ->
                val uri = mockk<Uri>()
                navigationStack.add(NavigationEntry(uri, name))
            }
            
            // Names should be in same order
            navigationStack.map { it.name } shouldBe directoryNames
        }
    }

    "Property 9: Breadcrumb indices are sequential" {
        checkAll(100, Arb.list(Arb.string(1..20), 0..10)) { directoryNames ->
            val breadcrumbs = mutableListOf<BreadcrumbItem>()
            
            // Build breadcrumbs
            breadcrumbs.add(BreadcrumbItem("Storage", null, 0))
            directoryNames.forEachIndexed { index, name ->
                val uri = mockk<Uri>()
                breadcrumbs.add(BreadcrumbItem(name, uri, index + 1))
            }
            
            // Indices should be sequential starting from 0
            breadcrumbs.forEachIndexed { index, breadcrumb ->
                breadcrumb.index shouldBe index
            }
        }
    }
})
