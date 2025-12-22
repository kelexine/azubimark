package com.kelexine.azubimark.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kelexine.azubimark.data.repository.AppPreferencesRepository
import com.kelexine.azubimark.data.repository.ThemePreferencesRepository
import com.kelexine.azubimark.domain.file.DocumentFileManager
import com.kelexine.azubimark.domain.markdown.MarkwonRenderer
import com.kelexine.azubimark.domain.theme.ThemeManager
import com.kelexine.azubimark.domain.theme.ThemeManagerImpl
import com.kelexine.azubimark.ui.about.AboutScreen
import com.kelexine.azubimark.ui.browser.FileBrowserScreen
import com.kelexine.azubimark.ui.browser.FileBrowserViewModel
import com.kelexine.azubimark.ui.home.HomeScreen
import com.kelexine.azubimark.ui.onboarding.OnboardingScreen
import com.kelexine.azubimark.ui.settings.ThemeSettingsScreen
import com.kelexine.azubimark.ui.splash.SplashScreen
import com.kelexine.azubimark.ui.theme.AzubiMarkTheme
import com.kelexine.azubimark.ui.viewer.MarkdownViewerScreen
import com.kelexine.azubimark.ui.viewer.MarkdownViewerViewModel
import com.kelexine.azubimark.util.EncodingDetector
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Main activity for AzubiMark.
 * 
 * Hosts the Compose navigation and handles external file intents.
 * Integrates ThemeManager for dynamic theming support.
 * Shows splash screen and onboarding on first launch.
 * 
 * Validates: Requirements 2.4
 */
class MainActivity : ComponentActivity() {

    // Dependencies (manual DI)
    private lateinit var themeManager: ThemeManager
    private lateinit var fileManager: DocumentFileManager
    private lateinit var markwonRenderer: MarkwonRenderer
    private lateinit var appPreferencesRepository: AppPreferencesRepository

    // State for pending file from external intent
    private var pendingFileUri: Uri? = null
    private var pendingFileContent: String? = null
    private var pendingFileName: String? = null
    
    // Keep splash screen visible while loading
    private var keepSplashScreen = true
    
    // State for permission request
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private var waitingForManageStoragePermission = false

    // Permission request launcher for legacy storage permission (API < 30)
    private val requestLegacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    // Folder picker launcher (SAF - works on all Android versions)
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Take persistable permission for long-term access
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)

        // Initialize dependencies
        initializeDependencies()

        // Handle incoming intent
        handleIntent(intent)

        setContent {
            var showCustomSplash by remember { mutableStateOf(true) }
            var showOnboarding by remember { mutableStateOf(false) }
            var showPermissionRequest by remember { mutableStateOf(false) }
            
            // Check onboarding state
            LaunchedEffect(Unit) {
                val onboardingCompleted = appPreferencesRepository.isOnboardingCompleted()
                showOnboarding = !onboardingCompleted
                keepSplashScreen = false
            }

            AzubiMarkTheme(themeManager = themeManager) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showCustomSplash -> {
                            SplashScreen(
                                onSplashComplete = {
                                    showCustomSplash = false
                                }
                            )
                        }
                        showOnboarding -> {
                            OnboardingScreen(
                                onComplete = {
                                    lifecycleScope.launch {
                                        appPreferencesRepository.setOnboardingCompleted()
                                    }
                                    showOnboarding = false
                                    // Show permission request after onboarding
                                    showPermissionRequest = true
                                }
                            )
                        }
                        showPermissionRequest -> {
                            PermissionRequestScreen(
                                onRequestPermission = {
                                    requestStoragePermission { 
                                        showPermissionRequest = false
                                    }
                                },
                                onSkip = {
                                    showPermissionRequest = false
                                }
                            )
                        }
                        else -> {
                            AzubiMarkApp(
                                themeManager = themeManager,
                                fileManager = fileManager,
                                markwonRenderer = markwonRenderer,
                                initialFileUri = pendingFileUri,
                                initialFileContent = pendingFileContent,
                                initialFileName = pendingFileName,
                                onOpenFolderPicker = { folderPickerLauncher.launch(null) },
                                onRequestStoragePermission = {
                                    requestStoragePermission { isGranted ->
                                        if (isGranted) {
                                            // Ideally retry loading file, but ViewModel handles retry
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Check if we were waiting for MANAGE_EXTERNAL_STORAGE permission
        if (waitingForManageStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            waitingForManageStoragePermission = false
            onPermissionResult?.invoke(Environment.isExternalStorageManager())
        }
    }

    private fun initializeDependencies() {
        val repository = ThemePreferencesRepository(this)
        themeManager = ThemeManagerImpl(this, repository, lifecycleScope)
        fileManager = DocumentFileManager(this)
        markwonRenderer = MarkwonRenderer(this)
        appPreferencesRepository = AppPreferencesRepository(this)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val uri = fileManager.handleExternalIntent(it)
            if (uri != null) {
                // Read the file content immediately while we have permission
                // External intents grant temporary permission that may expire
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes()
                    }
                    if (bytes != null) {
                        pendingFileContent = EncodingDetector.decodeWithFallback(bytes)
                            ?: EncodingDetector.decodeWithAutoDetect(bytes)
                        pendingFileName = uri.lastPathSegment ?: "Markdown File"
                        pendingFileUri = uri
                    }
                } catch (e: Exception) {
                    // If we can't read immediately, store the URI and try later
                    // This handles cases where the file is from SAF with persistent permission
                    pendingFileUri = uri
                    pendingFileContent = null
                    pendingFileName = null
                    
                    // Try to take persistable permission for SAF URIs
                    try {
                        val flags = it.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (flags != 0) {
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    } catch (e: SecurityException) {
                        // Permission might not be persistable, that's okay
                    }
                }
            }
        }
    }

    /**
     * Request storage permission based on Android version.
     * 
     * - Android 11+ (API 30+): Request MANAGE_EXTERNAL_STORAGE for full file access
     * - Android 10 and below (API < 30): Uses READ_EXTERNAL_STORAGE
     */
    private fun requestStoragePermission(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult
        
        when {
            // Android 11+ (API 30+): Check for MANAGE_EXTERNAL_STORAGE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    onResult(true)
                } else {
                    // Request MANAGE_EXTERNAL_STORAGE permission
                    try {
                        waitingForManageStoragePermission = true
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                        // Result will be checked in onResume
                    } catch (e: Exception) {
                        // Fallback to generic all files access settings
                        try {
                            waitingForManageStoragePermission = true
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            startActivity(intent)
                        } catch (e2: Exception) {
                            waitingForManageStoragePermission = false
                            onResult(false)
                        }
                    }
                }
            }
            // Android 10 and below (API < 30): Use READ_EXTERNAL_STORAGE
            else -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    onResult(true)
                } else {
                    requestLegacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    /**
     * Check if the app has storage permission.
     */
    fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}

/**
 * Permission request screen shown after onboarding.
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit
) {
    val isAndroid11Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = "Folder Icon",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "File Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (isAndroid11Plus) {
                "AzubiMark needs \"All files access\" permission to browse and open Markdown documents from your device storage."
            } else {
                "AzubiMark needs access to your files to browse and open Markdown documents."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (isAndroid11Plus) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You'll be taken to Settings to enable this permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isAndroid11Plus) "Open Settings" else "Grant Access")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "You can still open files using the document picker without granting full storage access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Navigation routes for the app.
 */
object Routes {
    const val HOME = "home"
    const val FILE_BROWSER = "file_browser"
    const val MARKDOWN_VIEWER = "markdown_viewer/{fileUri}"
    const val ABOUT = "about"
    const val SETTINGS = "settings"

    fun markdownViewer(fileUri: String): String {
        return "markdown_viewer/${URLEncoder.encode(fileUri, "UTF-8")}"
    }
}

/**
 * Main app composable with navigation.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun AzubiMarkApp(
    themeManager: ThemeManager,
    fileManager: DocumentFileManager,
    markwonRenderer: MarkwonRenderer,
    initialFileUri: Uri?,
    initialFileContent: String? = null,
    initialFileName: String? = null,
    onOpenFolderPicker: () -> Unit,
    onRequestStoragePermission: () -> Unit
) {
    val navController = rememberNavController()

    // ViewModels (created once and reused)
    val fileBrowserViewModel = remember {
        FileBrowserViewModel(fileManager)
    }

    val markdownViewerViewModel = remember {
        MarkdownViewerViewModel(fileManager, markwonRenderer)
    }

    // Handle initial file URI from external intent
    LaunchedEffect(initialFileUri) {
        initialFileUri?.let { uri ->
            if (initialFileContent != null) {
                // Use pre-loaded content (from external intent)
                markdownViewerViewModel.loadFileWithContent(
                    uri = uri,
                    content = initialFileContent,
                    fileName = initialFileName
                )
            }
            navController.navigate(Routes.markdownViewer(uri.toString()))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenFileBrowser = {
                    navController.navigate(Routes.FILE_BROWSER)
                },
                onOpenFile = { uri ->
                    navController.navigate(Routes.markdownViewer(uri.toString()))
                },
                onNavigateToAbout = {
                    navController.navigate(Routes.ABOUT)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        
        composable(Routes.FILE_BROWSER) {
            FileBrowserScreen(
                viewModel = fileBrowserViewModel,
                onFileSelected = { fileItem ->
                    navController.navigate(Routes.markdownViewer(fileItem.uri.toString()))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = {
                    navController.navigate(Routes.ABOUT)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.MARKDOWN_VIEWER,
            arguments = listOf(
                navArgument("fileUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("fileUri") ?: ""
            val fileUri = URLDecoder.decode(encodedUri, "UTF-8")
            val parsedUri = Uri.parse(fileUri)

            LaunchedEffect(fileUri) {
                // Only load if not already loaded (e.g., from external intent with pre-loaded content)
                if (markdownViewerViewModel.uiState.value.currentFile?.uri != parsedUri) {
                    markdownViewerViewModel.loadFile(parsedUri)
                }
            }

            MarkdownViewerScreen(
                viewModel = markdownViewerViewModel,
                markwonRenderer = markwonRenderer,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRequestPermission = onRequestStoragePermission
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SETTINGS) {
            ThemeSettingsScreen(
                themeManager = themeManager,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
