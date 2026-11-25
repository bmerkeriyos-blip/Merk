package com.example.core.firebase

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val HARNESS_PREFS = "shinobi_appcheck_harness_prefs"
private const val KEY_HARNESS_TOKEN_PROVIDED = "harness_debug_token_provided"

enum class SelectedAppCheckMode {
    DEBUG_HARNESS_INTENT,
    DEBUG_HARNESS_CACHED,
    DEBUG_EMULATOR_OR_TEST,
    RECAPTCHA_ENTERPRISE,
    NONE
}

fun configureAppCheck(context: Context, intent: Intent?): SelectedAppCheckMode {
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val siteKey = runCatching { context.getString(R.string.recaptcha_site_key) }.getOrNull()

        if (BuildConfig.DEBUG) {
            val harnessPrefs = context.getSharedPreferences(HARNESS_PREFS, Context.MODE_PRIVATE)
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit() is required before installing DebugAppCheckProviderFactory
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                harnessPrefs.edit()
                    .putBoolean(KEY_HARNESS_TOKEN_PROVIDED, true)
                    .commit()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed with launch intent debug token.")
                SelectedAppCheckMode.DEBUG_HARNESS_INTENT
            } else if (harnessPrefs.getBoolean(KEY_HARNESS_TOKEN_PROVIDED, false)) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed from cached harness debug token.")
                SelectedAppCheckMode.DEBUG_HARNESS_CACHED
            } else if (!siteKey.isNullOrBlank() && !isRobolectricRuntime() && !isEmulatorDevice()) {
                // On physical phones running a sideloaded Debug APK (e.g. from GitHub Actions) without
                // an allowlisted harness debug token, use reCAPTCHA Enterprise instead of an unregistered UUID.
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                // Warm up reCAPTCHA Enterprise client early so first AI/Auth call doesn't absorb init latency.
                runCatching {
                    FirebaseAppCheck.getInstance().getAppCheckToken(false)
                        .addOnFailureListener { err ->
                            Log.w(TAG, "Background App Check warm-up failed: ${err.message}")
                        }
                }
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed for physical device debug build.")
                SelectedAppCheckMode.RECAPTCHA_ENTERPRISE
            } else {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed for emulator/test environment.")
                SelectedAppCheckMode.DEBUG_EMULATOR_OR_TEST
            }
        } else {
            if (!siteKey.isNullOrBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                runCatching {
                    FirebaseAppCheck.getInstance().getAppCheckToken(false)
                        .addOnFailureListener { err ->
                            Log.w(TAG, "Release App Check warm-up failed: ${err.message}")
                        }
                }
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                SelectedAppCheckMode.RECAPTCHA_ENTERPRISE
            } else {
                Log.e(TAG, "recaptcha_site_key resource string is blank.")
                SelectedAppCheckMode.NONE
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
        SelectedAppCheckMode.NONE
    }
}

class AppCheckTimeoutException(message: String) : IllegalStateException(message)

/**
 * Verifies that Firebase App Check can mint a token within [timeoutMs] so AI calls fail fast
 * with an actionable attestation error rather than blocking silently for 60 seconds.
 */
suspend fun ensureAppCheckTokenReady(timeoutMs: Long = 15_000L) {
    if (isRobolectricRuntime()) return
    try {
        val result = withTimeout(timeoutMs) {
            FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
        }
        if (result.token.isBlank()) {
            throw IllegalStateException("Firebase App Check returned an empty token.")
        }
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw AppCheckTimeoutException(
            "Firebase App Check attestation timed out after ${timeoutMs / 1000}s. Check network & Google Play Services and retry."
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "App Check token retrieval failed", e)
        throw IllegalStateException(
            "Firebase App Check failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
            e
        )
    }
}

internal fun isRobolectricRuntime(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

internal fun isEmulatorDevice(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        "google_sdk" == Build.PRODUCT
