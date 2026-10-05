package com.example.data.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val lastSyncedFormatted: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class CloudSyncManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("bikecare_cloud_sync", Context.MODE_PRIVATE)
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        val lastSync = prefs.getLong("last_sync_time", 0L)
        if (lastSync > 0) {
            _syncState.value = SyncState.Success("Last synced: ${format(lastSync)}")
        }
    }

    suspend fun triggerSync(payloadSummary: String): Boolean {
        _syncState.value = SyncState.Syncing

        val now = System.currentTimeMillis()
        val remoteResult = runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) return@runCatching false
            val db = FirebaseFirestore.getInstance()
            val document = db.collection("bikecare_sync_events").document()
            document.set(
                mapOf(
                    "payloadSummary" to payloadSummary,
                    "createdAt" to now,
                    "platform" to "android"
                )
            ).await()
            true
        }.getOrDefault(false)

        if (remoteResult) {
            prefs.edit().putLong("last_sync_time", now).putBoolean("remote_sync", true).apply()
            _syncState.value = SyncState.Success("Cloud synced: ${format(now)}")
            return true
        }

        // Offline-safe fallback. No data is lost when Firebase is unavailable.
        prefs.edit()
            .putLong("last_sync_time", now)
            .putString("cloud_backup_data", payloadSummary)
            .putBoolean("remote_sync", false)
            .apply()

        _syncState.value = SyncState.Success("Offline backup saved: ${format(now)}")
        return false
    }

    fun getLastBackupData(): String? = prefs.getString("cloud_backup_data", null)

    fun getLastSyncTimeString(): String {
        val lastSync = prefs.getLong("last_sync_time", 0L)
        return if (lastSync > 0) format(lastSync) else "Not backed up yet"
    }

    private fun format(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}
