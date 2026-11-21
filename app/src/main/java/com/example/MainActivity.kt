package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.credentials.CredentialManager
import com.example.core.ai.AiMentorService
import com.example.core.firebase.configureAppCheck
import com.example.core.media.ShinobiAudioEngine
import com.example.core.notifications.ShinobiNotificationManager
import com.example.core.workers.CloudSyncWorker
import com.example.core.workers.DailyBriefingWorker
import com.example.data.local.ShinobiDatabase
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.repository.ShinobiRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.navigation.ShinobiAppShell
import com.example.ui.theme.ShinobiPlannerTheme
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.FocusTimerViewModel
import com.example.viewmodel.PlannerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private var audioEngine: ShinobiAudioEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Firebase App Check BEFORE setContent & any Firestore/AI calls
        configureAppCheck(this, intent)
        ShinobiNotificationManager.ensureChannels(this)

        val engine = ShinobiAudioEngine(applicationContext)
        audioEngine = engine

        setContent {
            ShinobiPlannerRoot(audioEngine = engine)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine?.release()
        audioEngine = null
    }
}

@Composable
private fun ShinobiPlannerRoot(audioEngine: ShinobiAudioEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val activeUser = currentUser
    if (activeUser == null) {
        ShinobiPlannerTheme {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        }
    } else {
        key(activeUser.uid) {
            val uid = activeUser.uid
            val db = remember(context) { ShinobiDatabase.getInstance(context.applicationContext) }
            val remote = remember(context) { FirestoreSyncRepository(context.applicationContext) }
            val repository = remember(uid) { ShinobiRepository(context.applicationContext, db, remote) }
            val aiService = remember { AiMentorService() }

            val plannerViewModel: PlannerViewModel = viewModel(
                key = "planner_$uid",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PlannerViewModel(repository, audioEngine, uid) as T
                    }
                }
            )

            val focusViewModel: FocusTimerViewModel = viewModel(
                key = "focus_$uid",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FocusTimerViewModel(repository, audioEngine, uid) as T
                    }
                }
            )

            val aiViewModel: AiViewModel = viewModel(
                key = "ai_$uid",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AiViewModel(repository, aiService, uid) as T
                    }
                }
            )

            LaunchedEffect(uid) {
                plannerViewModel.initializeSession(activeUser)
                CloudSyncWorker.schedulePeriodicSync(context.applicationContext)
                DailyBriefingWorker.schedule(context.applicationContext)
            }

            val credentialManager = remember(context) { CredentialManager.create(context) }

            ShinobiAppShell(
                plannerViewModel = plannerViewModel,
                focusViewModel = focusViewModel,
                aiViewModel = aiViewModel,
                onSignOut = {
                    audioEngine.pause()
                    signOutUser(credentialManager, scope)
                },
                onDeleteAccount = {
                    audioEngine.pause()
                    signOutUser(credentialManager, scope)
                }
            )
        }
    }
}
