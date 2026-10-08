package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.ProjectRepository
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.NoirTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Splash : Screen()
    object Home : Screen()
    object Editor : Screen()
}

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()
    private lateinit var projectRepository: ProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        projectRepository = ProjectRepository(applicationContext)

        setContent {
            NoirTheme {
                UmbraApp(
                    editorViewModel = editorViewModel,
                    projectRepository = projectRepository,
                    initialIntent = intent
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImageIntent(intent)
    }

    private fun handleImageIntent(intent: Intent) {
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                }
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        uri?.let {
            editorViewModel.initializeWithUri(it)
        }
    }
}

@Composable
fun UmbraApp(
    editorViewModel: EditorViewModel,
    projectRepository: ProjectRepository,
    initialIntent: Intent?
) {
    val initialHasImage = remember {
        val uri: Uri? = when (initialIntent?.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    initialIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    initialIntent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                }
            }
            Intent.ACTION_VIEW -> initialIntent.data
            else -> null
        }
        uri != null
    }

    var currentScreen by remember {
        mutableStateOf<Screen>(if (initialHasImage) Screen.Editor else Screen.Splash)
    }

    val savedProjects by projectRepository.projects.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        projectRepository.loadProjects()

        // Handle initial intent if app was opened with image
        val uri: Uri? = when (initialIntent?.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    initialIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    initialIntent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                }
            }
            Intent.ACTION_VIEW -> initialIntent.data
            else -> null
        }

        uri?.let {
            editorViewModel.initializeWithUri(it)
            currentScreen = Screen.Editor
        }
    }

    when (currentScreen) {
        is Screen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.Home -> {
            HomeScreen(
                savedProjects = savedProjects,
                onSelectImageUri = { uri ->
                    editorViewModel.initializeWithUri(uri)
                    currentScreen = Screen.Editor
                },
                onSelectSample = {
                    editorViewModel.initializeWithSample()
                    currentScreen = Screen.Editor
                },
                onOpenProject = { project ->
                    editorViewModel.initializeFromSavedProject(
                        imageUriStr = project.imageUri,
                        width = project.originalWidth,
                        height = project.originalHeight,
                        savedEditState = project.editState
                    )
                    currentScreen = Screen.Editor
                },
                onDeleteProject = { id ->
                    scope.launch { projectRepository.deleteProject(id) }
                }
            )
        }
        is Screen.Editor -> {
            EditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = {
                    scope.launch { projectRepository.loadProjects() }
                    currentScreen = Screen.Home
                }
            )
        }
    }
}
