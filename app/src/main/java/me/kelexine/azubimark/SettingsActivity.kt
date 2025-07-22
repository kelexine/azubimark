package me.kelexine.azubimark

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import me.kelexine.azubimark.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        themeManager = ThemeManager(this)
        themeManager.applyTheme()
        
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        // Load settings fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        private lateinit var themeManager: ThemeManager
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            themeManager = ThemeManager(requireContext())
            
            setupPreferences()
        }
        
        private fun setupPreferences() {
            // Theme preference
            val themePreference = findPreference<ListPreference>("theme")
            themePreference?.let { pref ->
                pref.setOnPreferenceChangeListener { _, newValue ->
                    val themeValue = (newValue as String).toInt()
                    themeManager.setTheme(themeValue)
                    activity?.recreate()
                    true
                }
                updateThemePreferenceSummary(pref)
            }
            
            // Font size preference
            val fontSizePreference = findPreference<SeekBarPreference>("font_size")
            fontSizePreference?.let { pref ->
                pref.setOnPreferenceChangeListener { _, _ ->
                    // Font size will be applied automatically through shared preferences
                    true
                }
            }
            
            // Line spacing preference
            val lineSpacingPreference = findPreference<SeekBarPreference>("line_spacing")
            lineSpacingPreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // Auto-scroll preference
            val autoScrollPreference = findPreference<SwitchPreferenceCompat>("auto_scroll_outline")
            autoScrollPreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // Syntax highlighting preference
            val syntaxHighlightingPreference = findPreference<SwitchPreferenceCompat>("syntax_highlighting")
            syntaxHighlightingPreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // Code block style preference
            val codeBlockStylePreference = findPreference<ListPreference>("code_block_style")
            codeBlockStylePreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // File browser start location
            val startLocationPreference = findPreference<ListPreference>("start_location")
            startLocationPreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // Show hidden files preference
            val showHiddenFilesPreference = findPreference<SwitchPreferenceCompat>("show_hidden_files")
            showHiddenFilesPreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // Reading mode preference
            val readingModePreference = findPreference<SwitchPreferenceCompat>("reading_mode")
            readingModePreference?.setOnPreferenceChangeListener { _, _ ->
                true
            }
            
            // About preference
            val aboutPreference = findPreference<Preference>("about")
            aboutPreference?.setOnPreferenceClickListener {
                startActivity(android.content.Intent(requireContext(), AboutActivity::class.java))
                true
            }
            
            // Reset settings preference
            val resetPreference = findPreference<Preference>("reset_settings")
            resetPreference?.setOnPreferenceClickListener {
                showResetDialog()
                true
            }
        }
        
        private fun updateThemePreferenceSummary(preference: ListPreference) {
            val currentTheme = themeManager.getCurrentTheme()
            val entries = resources.getStringArray(R.array.theme_entries)
            val values = resources.getStringArray(R.array.theme_values)
            
            val index = values.indexOf(currentTheme.toString())
            if (index >= 0 && index < entries.size) {
                preference.summary = entries[index]
            }
        }
        
        private fun showResetDialog() {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reset_settings_title)
                .setMessage(R.string.reset_settings_message)
                .setPositiveButton(R.string.reset) { _, _ ->
                    resetAllSettings()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        private fun resetAllSettings() {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            prefs.edit().clear().apply()
            
            // Reset theme manager preferences
            themeManager.setTheme(ThemeManager.THEME_SYSTEM)
            
            // Recreate the activity to apply changes
            activity?.recreate()
        }
        
        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }
        
        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }
        
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                "theme" -> {
                    val themePreference = findPreference<ListPreference>("theme")
                    themePreference?.let { updateThemePreferenceSummary(it) }
                }
            }
        }
    }
}