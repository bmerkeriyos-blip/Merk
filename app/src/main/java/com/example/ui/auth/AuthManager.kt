package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun ensureFirebaseInitialized(context: Context): Boolean {
    return runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context.applicationContext) != null
        } else {
            true
        }
    }.getOrDefault(false)
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
): Job? {
    if (!ensureFirebaseInitialized(context)) {
        onUnauthenticated()
        return null
    }
    if (runCatching { Firebase.auth.currentUser }.getOrNull() != null) {
        onAuthSuccess()
        return null
    }
    val activity = context.findActivity()
    if (credentialManager == null || activity == null) {
        onUnauthenticated()
        return null
    }
    val clientId = resolveWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return null
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    return scope.launch {
        try {
            withTimeout(12_000L) {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            onUnauthenticated()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
): Job? {
    if (!ensureFirebaseInitialized(context)) {
        onAuthError("Firebase is not initialized in this build (missing google-services.json).")
        return null
    }
    if (credentialManager == null) {
        onAuthError("Google Sign-In is unavailable on this device. Use Continue Offline to enter Shinobi Planner locally.")
        return null
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Google Sign-In requires an active screen window.")
        return null
    }
    val clientId = resolveWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id is blank.")
        return null
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    return scope.launch {
        var authStage = "opening Google account selector"
        try {
            val cred = withTimeout(90_000L) {
                credentialManager.getCredential(activity, request).credential
            }
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                authStage = "creating Firebase account"
                withTimeout(30_000L) {
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                }
                if (Firebase.auth.currentUser != null) {
                    onAuthSuccess()
                } else {
                    onAuthError("Firebase Authentication finished without an active user session. Please try again.")
                }
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google credentials available: ${e.message}", e)
            onAuthError("No Google account available on this device. Add a Google account in device Settings or check Google Play Services.")
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Google Credential Manager error [${e.type}]", e)
            onAuthError(e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.localizedMessage ?: "Google Sign-In failed (${e.type}).")
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            Log.e("Auth", "Google Sign-In timed out while $authStage", e)
            onAuthError("Google Sign-In timed out while $authStage. Check your internet connection & Google Play Services and retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            Log.e("Auth", "Firebase Auth failed [${e.errorCode}]", e)
            val detail = when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Google Sign-In is disabled in Firebase Authentication. Enable Google under Firebase Console -> Authentication -> Sign-in method."
                else -> e.localizedMessage ?: "Firebase Authentication failed (${e.errorCode})."
            }
            onAuthError(detail)
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed while $authStage", e)
            onAuthError(e.localizedMessage ?: "Sign in failed while $authStage. Please try again.")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager?,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { Firebase.auth.signOut() }
                onSignOutComplete()
            }
        }
    }
}

private fun resolveWebClientId(context: Context): String? {
    return runCatching { context.getString(R.string.default_web_client_id) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}
