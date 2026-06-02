package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

class MLockNotificationListenerService : NotificationListenerService(), SharedPreferences.OnSharedPreferenceChangeListener {
    
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        cancelLockedNotifications()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "Hide Notifications" || key == "locked_apps" || key == "current_unlocked_app") {
            cancelLockedNotifications()
        }
    }

    private fun isCancelable(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == this.packageName) return false
        val isForeground = (sbn.notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE) != 0
        val isOngoing = (sbn.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        return !isForeground && !isOngoing
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun postSubstituteNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "locked_apps_notifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Protected Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        var appName = packageName
        try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {}

        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("package_name", packageName)
            putExtra("from_notification", true)
            putExtra("original_intent", sbn.notification.contentIntent)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            packageName.hashCode(), 
            lockIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val substitute = NotificationCompat.Builder(this, channelId)
            .setContentTitle(appName)
            .setContentText("Unlock to see the message")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(packageName, 1000, substitute)
    }

    private fun isAppLocked(packageName: String): Boolean {
        val hideNotifications = prefs.getBoolean("Hide Notifications", false)
        if (!hideNotifications) return false
        
        val lockedApps = prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()
        if (!lockedApps.contains(packageName)) return false
        
        val unlockedApp = prefs.getString("current_unlocked_app", null)
        if (unlockedApp != packageName) return true // Definitely locked
        
        val lastForeground = prefs.getString("last_foreground_package", "")
        if (lastForeground == packageName) return false // User is actively inside the app
        
        // They are outside the app. Let's check grace period.
        val leaveTime = prefs.getLong("leave_time_$packageName", 0L)
        val autoLockTimeRaw = prefs.getString("auto_lock_time", "Immediately")
        val autoLockTimeMs = when (autoLockTimeRaw) {
            "Immediately" -> 0L
            "1 Second" -> 1000L
            "After 5 Seconds" -> 5000L
            "After 10 Seconds" -> 10000L
            "After 30 Seconds" -> 30000L
            "After 1 Minute" -> 60000L
            "After 5 Minutes" -> 300000L
            "After 10 Minutes" -> 600000L
            else -> 0L
        }
        
        val effectiveGracePeriod = if (autoLockTimeMs == 0L) 500L else autoLockTimeMs
        
        if (System.currentTimeMillis() - leaveTime < effectiveGracePeriod) {
            return false // Still in grace period
        }
        
        return true // Grace period expired, it is locked
    }

    private fun cancelLockedNotifications() {
        try {
            val activeNotifs = activeNotifications ?: return
            for (sbn in activeNotifs) {
                if (isAppLocked(sbn.packageName) && isCancelable(sbn)) {
                    cancelNotification(sbn.key)
                    postSubstituteNotification(sbn)
                    Log.d("MLockNotification", "Cleared existing notification for: ${sbn.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.e("MLockNotification", "Error fetching active notifications", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val packageName = sbn.packageName
        
        if (isAppLocked(packageName) && isCancelable(sbn)) {
            cancelNotification(sbn.key)
            postSubstituteNotification(sbn)
            Log.d("MLockNotification", "Replaced new notification for locked app: $packageName")
        }
    }
}
