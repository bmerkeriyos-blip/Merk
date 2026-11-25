package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.media.ShinobiAudioEngine
import com.example.core.notifications.ShinobiNotificationManager
import com.example.core.workers.DailyBriefingWorker
import com.example.data.local.ShinobiDatabase
import com.example.data.repository.ShinobiRepository
import com.example.ui.navigation.ShinobiAppShell
import com.example.viewmodel.FocusTimerViewModel
import com.example.viewmodel.PlannerViewModel

class MainActivity : FragmentActivity() {

    private var audioEngine: ShinobiAudioEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val db = remember(context) { ShinobiDatabase.getInstance(context.applicationContext) }
    val repository = remember(context, db) { ShinobiRepository(context.applicationContext, db) }

    var activeUid by remember(context) {
        mutableStateOf(
            ShinobiRepository.getCachedLocalUid(context) ?: ShinobiRepository.LOCAL_GUEST_UID
        )
    }

    LaunchedEffect(repository) {
        val resolvedUid = repository.resolveActiveLocalUid()
        if (resolvedUid != activeUid) {
            activeUid = resolvedUid
        }
    }

    key(activeUid) {
        val plannerViewModel: PlannerViewModel = viewModel(
            key = "planner_$activeUid",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlannerViewModel(repository, audioEngine, activeUid) as T
                }
            }
        )

        val focusViewModel: FocusTimerViewModel = viewModel(
            key = "focus_$activeUid",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FocusTimerViewModel(repository, audioEngine, activeUid) as T
                }
            }
        )

        LaunchedEffect(activeUid) {
            plannerViewModel.initializeSession()
            runCatching { DailyBriefingWorker.schedule(context.applicationContext) }
        }

        ShinobiAppShell(
            plannerViewModel = plannerViewModel,
            focusViewModel = focusViewModel
        )
    }
}
