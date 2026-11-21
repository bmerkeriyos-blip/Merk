package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Firestore." }
        return uid
    }

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "doc_${System.currentTimeMillis()}" }

    fun observeRemoteTasks(userId: String): Flow<List<CloudTask>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(profile: UserProfileCloud): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mapOf(
                    "displayName" to profile.displayName.take(100),
                    "username" to profile.username.take(60),
                    "email" to profile.email.take(150),
                    "university" to profile.university.take(120),
                    "department" to profile.department.take(120),
                    "academicYear" to profile.academicYear.take(40),
                    "xp" to profile.xp.coerceAtLeast(0L),
                    "level" to profile.level.coerceAtLeast(1L),
                    "ninjaTitle" to profile.ninjaTitle.take(60),
                    "currentStreak" to profile.currentStreak.coerceAtLeast(0L),
                    "longestStreak" to profile.longestStreak.coerceAtLeast(0L),
                    "onboardingCompleted" to profile.onboardingCompleted,
                    "photoUrl" to profile.photoUrl?.take(500),
                    "lastActiveDate" to profile.lastActiveDate?.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(updates).await()
            } else {
                docRef.set(profile.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(userId: String): Result<UserProfileCloud?> {
        return try {
            val uid = requireUserId()
            check(uid == userId) { "User mismatch" }
            val snap = db.collection("users").document(uid).get().await()
            val profile = snap.toObject(UserProfileCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun upsertTask(taskId: String, task: CloudTask): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = sanitizeId(taskId)
        val docRef = db.collection("users").document(uid).collection("tasks").document(safeId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mapOf(
                    "title" to task.title.take(200),
                    "notes" to task.notes?.take(2000),
                    "subjectName" to task.subjectName?.take(100),
                    "category" to task.category,
                    "dateIso" to task.dateIso.take(20),
                    "startTimeMinutes" to task.startTimeMinutes.coerceIn(0L, 1440L),
                    "endTimeMinutes" to task.endTimeMinutes.coerceIn(0L, 1440L),
                    "priority" to task.priority,
                    "missionRank" to task.missionRank,
                    "recurrence" to task.recurrence,
                    "location" to task.location?.take(150),
                    "reminderOffsets" to task.reminderOffsets?.take(100),
                    "xpReward" to task.xpReward.coerceIn(0L, 5000L),
                    "colorHex" to task.colorHex.take(20),
                    "completed" to task.completed,
                    "subtasksSummary" to task.subtasksSummary?.take(2000),
                    "completedAtIso" to task.completedAtIso?.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(updates).await()
            } else {
                docRef.set(task.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = sanitizeId(taskId)
        val docRef = db.collection("users").document(uid).collection("tasks").document(safeId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertSubject(subjectId: String, subject: CloudSubject): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = sanitizeId(subjectId)
        val docRef = db.collection("users").document(uid).collection("subjects").document(safeId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mapOf(
                    "name" to subject.name.take(120),
                    "code" to subject.code.take(40),
                    "instructor" to subject.instructor?.take(120),
                    "room" to subject.room?.take(80),
                    "colorHex" to subject.colorHex.take(20),
                    "targetHoursPerWeek" to subject.targetHoursPerWeek.coerceIn(0L, 168L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(updates).await()
            } else {
                docRef.set(subject.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun insertStudySession(sessionId: String, session: CloudStudySession): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = sanitizeId(sessionId)
        val docRef = db.collection("users").document(uid).collection("studySessions").document(safeId)
        return try {
            docRef.set(session.copy(userId = uid).toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertNote(noteId: String, note: CloudNote): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = sanitizeId(noteId)
        val docRef = db.collection("users").document(uid).collection("notes").document(safeId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val updates = mapOf(
                    "title" to note.title.take(200),
                    "content" to note.content.take(10000),
                    "subjectName" to note.subjectName.take(120),
                    "linkedTaskId" to note.linkedTaskId?.take(128),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(updates).await()
            } else {
                docRef.set(note.copy(userId = uid).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createBackupSnapshot(summary: String, taskCount: Int, xpSnapshot: Int): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val backupId = sanitizeId("bk_${System.currentTimeMillis()}")
        val docRef = db.collection("users").document(uid).collection("backups").document(backupId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "summary" to summary.take(500),
                "taskCount" to taskCount.toLong(),
                "xpSnapshot" to xpSnapshot.toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserCloudData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        return try {
            val userDoc = db.collection("users").document(uid)
            val subcollections = listOf(
                "tasks", "subjects", "studySessions", "achievements",
                "settings", "chats", "chatMessages", "notes", "backups"
            )
            for (col in subcollections) {
                val snap = userDoc.collection(col).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
