package com.kelexine.azubimark.property

import com.kelexine.azubimark.data.model.ThemePreferences
import com.kelexine.azubimark.data.model.ThemeType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll

/**
 * Property-based test for theme preference persistence.
 * 
 * Feature: azubimark-android-app, Property 12: Theme Preference Persistence
 * Validates: Requirements 3.5
 * 
 * Tests that for any theme preference setting, the data model correctly
 * represents and preserves the values. The actual DataStore persistence
 * is tested via integration tests on a real device.
 */
class ThemePreferencePersistenceTest : StringSpec({
    
    /**
     * Property 12: Theme Preference Persistence
     * 
     * For any theme preference setting, the ThemePreferences data class
     * should correctly store and retrieve all values.
     */
    "Property 12: Theme preferences data model preserves all values" {
        checkAll(
            100,
            Arb.enum<ThemeType>(),
            Arb.boolean(),
            Arb.long().orNull()
        ) { themeType, isDynamicColors, customAccentColor ->
            // Create a ThemePreferences instance
            val preferences = ThemePreferences(
                themeType = themeType,
                isDynamicColors = isDynamicColors,
                customAccentColor = customAccentColor
            )
            
            // Verify all values are correctly stored
            preferences.themeType shouldBe themeType
            preferences.isDynamicColors shouldBe isDynamicColors
            preferences.customAccentColor shouldBe customAccentColor
        }
    }
    
    /**
     * Property 12 (continued): Theme preferences copy preserves values
     * 
     * For any theme preference, copying with modifications should preserve
     * unmodified values and update modified values.
     */
    "Property 12: Theme preferences copy preserves unmodified values" {
        checkAll(
            100,
            Arb.enum<ThemeType>(),
            Arb.boolean(),
            Arb.long().orNull(),
            Arb.enum<ThemeType>()
        ) { themeType, isDynamicColors, customAccentColor, newThemeType ->
            val original = ThemePreferences(
                themeType = themeType,
                isDynamicColors = isDynamicColors,
                customAccentColor = customAccentColor
            )
            
            // Copy with only themeType changed
            val modified = original.copy(themeType = newThemeType)
            
            // Verify the modified value changed
            modified.themeType shouldBe newThemeType
            
            // Verify unmodified values are preserved
            modified.isDynamicColors shouldBe isDynamicColors
            modified.customAccentColor shouldBe customAccentColor
        }
    }
    
    /**
     * Property 12 (continued): Theme preferences equality
     * 
     * Two ThemePreferences with the same values should be equal.
     */
    "Property 12: Theme preferences with same values are equal" {
        checkAll(
            100,
            Arb.enum<ThemeType>(),
            Arb.boolean(),
            Arb.long().orNull()
        ) { themeType, isDynamicColors, customAccentColor ->
            val preferences1 = ThemePreferences(
                themeType = themeType,
                isDynamicColors = isDynamicColors,
                customAccentColor = customAccentColor
            )
            
            val preferences2 = ThemePreferences(
                themeType = themeType,
                isDynamicColors = isDynamicColors,
                customAccentColor = customAccentColor
            )
            
            preferences1 shouldBe preferences2
            preferences1.hashCode() shouldBe preferences2.hashCode()
        }
    }
    
    /**
     * Property 12 (continued): Default theme preferences
     * 
     * Default ThemePreferences should have sensible defaults.
     */
    "Property 12: Default theme preferences have correct defaults" {
        val defaultPreferences = ThemePreferences()
        
        defaultPreferences.themeType shouldBe ThemeType.SYSTEM
        defaultPreferences.isDynamicColors shouldBe true
        defaultPreferences.customAccentColor shouldBe null
    }
})
