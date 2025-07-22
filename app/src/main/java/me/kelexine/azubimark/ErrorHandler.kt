package me.kelexine.azubimark

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized error handling and user feedback system for AzubiMark
 */
object ErrorHandler {
    
    private const val TAG = "AzubiMark_ErrorHandler"
    
    /**
     * Error types for categorized handling
     */
    enum class ErrorType {
        FILE_READ_ERROR,
        PERMISSION_ERROR,
        NETWORK_ERROR,
        PARSING_ERROR,
        STORAGE_ERROR,
        UI_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * Handle exceptions with appropriate user feedback
     */
    fun handleError(
        context: Context,
        error: Throwable,
        errorType: ErrorType = ErrorType.UNKNOWN_ERROR,
        userMessage: String? = null,
        showDialog: Boolean = false
    ) {
        // Log the error
        logError(error, errorType)
        
        // Generate user-friendly message
        val message = userMessage ?: generateUserMessage(errorType, error)
        
        // Show feedback to user
        if (showDialog && context is AppCompatActivity) {
            showErrorDialog(context, message, error)
        } else {
            showErrorSnackbar(context, message)
        }
    }
    
    /**
     * Handle file operation errors specifically
     */
    fun handleFileError(
        context: Context,
        error: Throwable,
        fileName: String? = null,
        showDialog: Boolean = true
    ) {
        val message = when {
            fileName != null -> "Failed to process file '$fileName': ${getSimpleErrorMessage(error)}"
            else -> "File operation failed: ${getSimpleErrorMessage(error)}"
        }
        
        handleError(context, error, ErrorType.FILE_READ_ERROR, message, showDialog)
    }
    
    /**
     * Handle network-related errors
     */
    fun handleNetworkError(
        context: Context,
        error: Throwable,
        operation: String = "network operation"
    ) {
        val message = "Network error during $operation. Please check your internet connection."
        handleError(context, error, ErrorType.NETWORK_ERROR, message, false)
    }
    
    /**
     * Handle permission-related errors
     */
    fun handlePermissionError(
        context: Context,
        error: Throwable,
        permissionType: String = "required permission"
    ) {
        val message = "Permission denied for $permissionType. Please grant the necessary permissions in Settings."
        handleError(context, error, ErrorType.PERMISSION_ERROR, message, true)
    }
    
    /**
     * Log error with detailed information
     */
    private fun logError(error: Throwable, errorType: ErrorType) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stackTrace = getStackTrace(error)
        
        Log.e(TAG, "[$timestamp] Error Type: $errorType")
        Log.e(TAG, "Error Message: ${error.message}")
        Log.e(TAG, "Stack Trace: $stackTrace")
        
        // In a production app, you might want to send this to a crash reporting service
        // like Firebase Crashlytics or Bugsnag
    }
    
    /**
     * Generate user-friendly error messages
     */
    private fun generateUserMessage(errorType: ErrorType, error: Throwable): String {
        return when (errorType) {
            ErrorType.FILE_READ_ERROR -> "Unable to read file. The file might be corrupted or in an unsupported format."
            ErrorType.PERMISSION_ERROR -> "Permission required. Please grant storage access to browse files."
            ErrorType.NETWORK_ERROR -> "Network connection error. Please check your internet connection."
            ErrorType.PARSING_ERROR -> "Unable to parse content. The file format might not be supported."
            ErrorType.STORAGE_ERROR -> "Storage access error. Please ensure sufficient storage space."
            ErrorType.UI_ERROR -> "Interface error occurred. Please restart the app if the problem persists."
            ErrorType.UNKNOWN_ERROR -> "An unexpected error occurred: ${getSimpleErrorMessage(error)}"
        }
    }
    
    /**
     * Get a simplified error message for users
     */
    private fun getSimpleErrorMessage(error: Throwable): String {
        return when (error) {
            is java.io.FileNotFoundException -> "File not found"
            is java.io.IOException -> "File access error"
            is SecurityException -> "Permission denied"
            is OutOfMemoryError -> "Insufficient memory"
            is IllegalArgumentException -> "Invalid input"
            else -> error.message?.take(50) ?: "Unknown error"
        }
    }
    
    /**
     * Show error dialog for critical errors
     */
    private fun showErrorDialog(context: AppCompatActivity, message: String, error: Throwable) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Details") { _, _ ->
                showErrorDetailsDialog(context, error)
            }
            .show()
    }
    
    /**
     * Show detailed error information
     */
    private fun showErrorDetailsDialog(context: AppCompatActivity, error: Throwable) {
        val details = buildString {
            appendLine("Error Type: ${error.javaClass.simpleName}")
            appendLine("Message: ${error.message}")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("\nStack Trace:")
            appendLine(getStackTrace(error))
        }
        
        MaterialAlertDialogBuilder(context)
            .setTitle("Error Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .setNeutralButton("Share") { _, _ ->
                shareErrorReport(context, details)
            }
            .show()
    }
    
    /**
     * Show error snackbar for non-critical errors
     */
    private fun showErrorSnackbar(context: Context, message: String) {
        if (context is AppCompatActivity) {
            val rootView = context.findViewById<android.view.View>(android.R.id.content)
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                .setAction("Dismiss") { }
                .show()
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Get full stack trace as string
     */
    private fun getStackTrace(error: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        error.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * Share error report via system sharing
     */
    private fun shareErrorReport(context: Context, errorDetails: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AzubiMark Error Report")
            putExtra(Intent.EXTRA_TEXT, "AzubiMark Error Report\n\n$errorDetails")
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share Error Report")
        context.startActivity(chooser)
    }
    
    /**
     * Safe execution wrapper that handles exceptions
     */
    fun <T> safeExecute(
        context: Context,
        errorType: ErrorType = ErrorType.UNKNOWN_ERROR,
        operation: () -> T
    ): T? {
        return try {
            operation()
        } catch (e: Exception) {
            handleError(context, e, errorType)
            null
        }
    }
    
    /**
     * Async safe execution wrapper
     */
    suspend fun <T> safeExecuteAsync(
        context: Context,
        errorType: ErrorType = ErrorType.UNKNOWN_ERROR,
        operation: suspend () -> T
    ): T? {
        return try {
            operation()
        } catch (e: Exception) {
            handleError(context, e, errorType)
            null
        }
    }
    
    /**
     * Initialize global exception handler for uncaught exceptions
     */
    fun initializeGlobalErrorHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", exception)
            
            // Log the crash
            logError(exception, ErrorType.UNKNOWN_ERROR)
            
            // Call the default handler to let the system handle the crash
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}