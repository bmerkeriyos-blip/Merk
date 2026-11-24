package com.example

import android.content.Context
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
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutUser
import com.example.ui.navigation.ShinobiAppShell
import com.example.ui.theme.ShinobiPlannerTheme
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.FocusTimerViewModel
import com.example.viewmodel.PlannerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : FragmentActivity() {

    private var audioEngine: ShinobiAudioEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check as the first call in onCreate before any Firebase/UI calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
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
    val authPrefs = remember(context) {
        context.applicationContext.getSharedPreferences("shinobi_auth_prefs", Context.MODE_PRIVATE)
    }

    var currentUser by remember {
        mutableStateOf(runCatching { Firebase.auth.currentUser }.getOrNull())
    }
    var skippedSignIn by remember {
        mutableStateOf(authPrefs.getBoolean("skipped_sign_in", false))
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        val firebaseAuth = runCatching { Firebase.auth }.getOrNull()
        runCatching { firebaseAuth?.addAuthStateListener(listener) }
        onDispose { runCatching { firebaseAuth?.removeAuthStateListener(listener) } }
    }

    val activeUser = currentUser
    if (activeUser == null && !skippedSignIn) {
        ShinobiPlannerTheme {
            AuthScreen(
                onAuthSuccess = {
                    authPrefs.edit().putBoolean("skipped_sign_in", false).apply()
                    skippedSignIn = false
                    currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                },
                onSkipForNow = {
                    authPrefs.edit().putBoolean("skipped_sign_in", true).apply()
                    skippedSignIn = true
                }
            )
        }
    } else {
        val isSignedInWithGoogle = activeUser != null
        val uid = activeUser?.uid ?: ShinobiRepository.LOCAL_GUEST_UID
        key(uid, isSignedInWithGoogle) {
            val db = remember(context) { ShinobiDatabase.getInstance(context.applicationContext) }
            val remote = remember(context, isSignedInWithGoogle) {
                if (isSignedInWithGoogle) {
                    runCatching { FirestoreSyncRepository(context.applicationContext) }.getOrNull()
                } else null
            }
            val repository = remember(uid, remote) {
                ShinobiRepository(context.applicationContext, db, remote)
            }
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

            LaunchedEffect(uid, isSignedInWithGoogle) {
                plannerViewModel.initializeSession(activeUser)
                if (isSignedInWithGoogle) {
                    runCatching { CloudSyncWorker.schedulePeriodicSync(context.applicationContext) }
                }
                runCatching { DailyBriefingWorker.schedule(context.applicationContext) }
            }

            val credentialManager = remember(context) {
                runCatching { CredentialManager.create(context) }.getOrNull()
            }

            ShinobiAppShell(
                plannerViewModel = plannerViewModel,
                focusViewModel = focusViewModel,
                aiViewModel = aiViewModel,
                isSignedInWithGoogle = isSignedInWithGoogle,
                onSignInWithGoogle = {
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            authPrefs.edit().putBoolean("skipped_sign_in", false).apply()
                            skippedSignIn = false
                            currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                        },
                        onAuthError = { msg ->
                            plannerViewModel.postStatusMessage(msg)
                        },
                        scope = scope
                    )
                },
                onSignOut = {
                    audioEngine.pause()
                    authPrefs.edit().putBoolean("skipped_sign_in", false).apply()
                    skippedSignIn = false
                    signOutUser(credentialManager, scope)
                },
                onDeleteAccount = {
                    audioEngine.pause()
                    authPrefs.edit().putBoolean("skipped_sign_in", false).apply()
                    skippedSignIn = false
                    signOutUser(credentialManager, scope)
                }
            )
        }
    }
}
