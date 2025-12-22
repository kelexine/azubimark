package com.kelexine.azubimark.property

import com.kelexine.azubimark.ui.browser.ScreenBreakpoints
import com.kelexine.azubimark.ui.browser.ScreenSizeCategory
import com.kelexine.azubimark.ui.browser.calculateColumnCount
import com.kelexine.azubimark.ui.browser.shouldUseGridLayout
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Property-based tests for responsive layout adaptation.
 *
 * Feature: azubimark-android-app, Property 16: Responsive Layout Adaptation
 * Validates: Requirements 5.5
 *
 * Tests that UI adapts appropriately to screen size and orientation changes.
 */
class ResponsiveLayoutPropertiesTest : StringSpec({

    /**
     * Property 16: Responsive Layout Adaptation
     *
     * For any screen size or orientation change, the user interface should
     * adapt appropriately while maintaining functionality and readability.
     *
     * Validates: Requirements 5.5
     */
    "Property 16: Compact screens use single column layout" {
        checkAll(100, Arb.int(0..599)) { screenWidthDp ->
            val columnCount = calculateColumnCount(screenWidthDp)
            val useGrid = shouldUseGridLayout(screenWidthDp)
            val category = ScreenBreakpoints.getCategory(screenWidthDp)
            
            columnCount shouldBe 1
            useGrid shouldBe false
            category shouldBe ScreenSizeCategory.COMPACT
        }
    }

    "Property 16: Medium screens use 2 column grid layout" {
        checkAll(100, Arb.int(600..839)) { screenWidthDp ->
            val columnCount = calculateColumnCount(screenWidthDp)
            val useGrid = shouldUseGridLayout(screenWidthDp)
            val category = ScreenBreakpoints.getCategory(screenWidthDp)
            
            columnCount shouldBe 2
            useGrid shouldBe true
            category shouldBe ScreenSizeCategory.MEDIUM
        }
    }

    "Property 16: Expanded screens use 3 column grid layout" {
        checkAll(100, Arb.int(840..2000)) { screenWidthDp ->
            val columnCount = calculateColumnCount(screenWidthDp)
            val useGrid = shouldUseGridLayout(screenWidthDp)
            val category = ScreenBreakpoints.getCategory(screenWidthDp)
            
            columnCount shouldBe 3
            useGrid shouldBe true
            category shouldBe ScreenSizeCategory.EXPANDED
        }
    }

    "Property 16: Column count is always positive" {
        checkAll(100, Arb.int(0..3000)) { screenWidthDp ->
            val columnCount = calculateColumnCount(screenWidthDp)
            
            (columnCount > 0) shouldBe true
        }
    }

    "Property 16: Column count is bounded between 1 and 3" {
        checkAll(100, Arb.int(0..3000)) { screenWidthDp ->
            val columnCount = calculateColumnCount(screenWidthDp)
            
            (columnCount in 1..3) shouldBe true
        }
    }

    "Property 16: Grid layout is used for medium and expanded screens" {
        checkAll(100, Arb.int(600..3000)) { screenWidthDp ->
            val useGrid = shouldUseGridLayout(screenWidthDp)
            
            useGrid shouldBe true
        }
    }

    "Property 16: List layout is used for compact screens" {
        checkAll(100, Arb.int(0..599)) { screenWidthDp ->
            val useGrid = shouldUseGridLayout(screenWidthDp)
            
            useGrid shouldBe false
        }
    }

    "Property 16: Screen breakpoints are correctly defined" {
        ScreenBreakpoints.COMPACT shouldBe 0
        ScreenBreakpoints.MEDIUM shouldBe 600
        ScreenBreakpoints.EXPANDED shouldBe 840
    }

    "Property 16: Category transitions at correct breakpoints" {
        // Just below medium breakpoint
        ScreenBreakpoints.getCategory(599) shouldBe ScreenSizeCategory.COMPACT
        
        // At medium breakpoint
        ScreenBreakpoints.getCategory(600) shouldBe ScreenSizeCategory.MEDIUM
        
        // Just below expanded breakpoint
        ScreenBreakpoints.getCategory(839) shouldBe ScreenSizeCategory.MEDIUM
        
        // At expanded breakpoint
        ScreenBreakpoints.getCategory(840) shouldBe ScreenSizeCategory.EXPANDED
    }

    "Property 16: Layout consistency across similar screen sizes" {
        checkAll(100, Arb.int(0..2000)) { screenWidthDp ->
            val category1 = ScreenBreakpoints.getCategory(screenWidthDp)
            val category2 = ScreenBreakpoints.getCategory(screenWidthDp)
            
            // Same screen width should always produce same category
            category1 shouldBe category2
        }
    }

    "Property 16: Column count matches category" {
        checkAll(100, Arb.int(0..2000)) { screenWidthDp ->
            val category = ScreenBreakpoints.getCategory(screenWidthDp)
            val columnCount = calculateColumnCount(screenWidthDp)
            
            when (category) {
                ScreenSizeCategory.COMPACT -> columnCount shouldBe 1
                ScreenSizeCategory.MEDIUM -> columnCount shouldBe 2
                ScreenSizeCategory.EXPANDED -> columnCount shouldBe 3
            }
        }
    }

    "Property 16: Grid usage matches category" {
        checkAll(100, Arb.int(0..2000)) { screenWidthDp ->
            val category = ScreenBreakpoints.getCategory(screenWidthDp)
            val useGrid = shouldUseGridLayout(screenWidthDp)
            
            when (category) {
                ScreenSizeCategory.COMPACT -> useGrid shouldBe false
                ScreenSizeCategory.MEDIUM -> useGrid shouldBe true
                ScreenSizeCategory.EXPANDED -> useGrid shouldBe true
            }
        }
    }

    "Property 16: All screen size categories are covered" {
        val categories = ScreenSizeCategory.values()
        
        categories.size shouldBe 3
        categories.contains(ScreenSizeCategory.COMPACT) shouldBe true
        categories.contains(ScreenSizeCategory.MEDIUM) shouldBe true
        categories.contains(ScreenSizeCategory.EXPANDED) shouldBe true
    }
})
