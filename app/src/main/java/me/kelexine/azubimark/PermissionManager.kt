package me.kelexine.azubimark

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Manages runtime permissions for the AzubiMark app
 * Handles both legacy external storage permissions and new media permissions for Android 13+
 */
class PermissionManager(
    private val activity: AppCompatActivity,
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit
) {
    
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    /**
     * Initialize the permission launcher - call this in onCreate()
     */
    fun initializePermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                onPermissionGranted()
            } else {
                handlePermissionDenied(permissions)
            }
        }
    }
    
    /**
     * Check and request storage permissions based on Android version
     */
    fun checkStoragePermissions() {
        val requiredPermissions = getRequiredStoragePermissions()
        
        if (hasAllPermissions(requiredPermissions)) {
            onPermissionGranted()
            return
        }
        
        // Check if we should show rationale
        if (shouldShowRationale(requiredPermissions)) {
            showPermissionRationale(requiredPermissions)
        } else {
            requestPermissions(requiredPermissions)
        }
    }
    
    /**
     * Get required storage permissions based on Android version
     */
    private fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            arrayOf(
                "android.permission.READ_MEDIA_DOCUMENTS",
                Manifest.permission.READ_EXTERNAL_STORAGE // Still needed for some cases
            )
        } else {
            // Pre-Android 13 uses broad external storage permission
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    private fun hasAllPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if we should show permission rationale for any of the permissions
     */
    private fun shouldShowRationale(permissions: Array<String>): Boolean {
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * Show permission rationale dialog
     */
    private fun showPermissionRationale(permissions: Array<String>) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Storage Permission Required")
            .setMessage("AzubiMark needs access to your device's storage to browse and open Markdown files. This permission allows you to:\n\n• Browse files on your device\n• Open Markdown documents from any location\n• Access files from external apps\n\nYour privacy is important to us - we only access files you explicitly choose to open.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissions(permissions)
            }
            .setNegativeButton("Cancel") { _, _ ->
                onPermissionDenied()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Request the specified permissions
     */
    private fun requestPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }
    
    /**
     * Handle permission denied scenario
     */
    private fun handlePermissionDenied(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys.toList()
        
        // Check if any permissions were permanently denied
        val permanentlyDenied = deniedPermissions.any { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
        
        if (permanentlyDenied) {
            showPermanentlyDeniedDialog()
        } else {
            showPermissionDeniedSnackbar()
        }
        
        onPermissionDenied()
    }
    
    /**
     * Show dialog for permanently denied permissions
     */
    private fun showPermanentlyDeniedDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permission Required")
            .setMessage("Storage permission has been permanently denied. To use file browsing features, please enable the permission in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Show snackbar for permission denied
     */
    private fun showPermissionDeniedSnackbar() {
        val rootView = activity.findViewById<android.view.View>(android.R.id.content)
        Snackbar.make(rootView, "Storage permission is required for file browsing", Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                checkStoragePermissions()
            }
            .show()
    }
    
    /**
     * Open app settings
     */
    private fun openAppSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = android.net.Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }
    
    companion object {
        /**
         * Check if the app has storage permissions without creating a PermissionManager instance
         */
        fun hasStoragePermissions(context: Context): Boolean {
            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    "android.permission.READ_MEDIA_DOCUMENTS",
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            
            return requiredPermissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}