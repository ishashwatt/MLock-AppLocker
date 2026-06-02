package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MLockAccessibilityService : AccessibilityService() {
    private var lastForegroundPackage = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            val className = event.className?.toString() ?: ""
            
            Log.d("MLockService", "App opened: $packageName, class: $className")
            
            if (packageName.isEmpty() || packageName == this.packageName) return
            
            if (className.contains("Toast")) {
                return
            }
            
            // Ignore system packages for reset to avoid losing unlock state abruptly
            val ignoredPackages = setOf(
                "com.android.systemui",
                "com.google.android.inputmethod.latin",
                "com.google.android.inputmethod.pinyin",
                "com.samsung.android.honeyboard"
            )
            if (ignoredPackages.contains(packageName)) return
            
            val prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
            val lockedApps = prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()
            val unlockedApp = prefs.getString("current_unlocked_app", null)

            if (packageName != lastForegroundPackage) {
                // App switched!
                val previousApp = lastForegroundPackage
                lastForegroundPackage = packageName
                prefs.edit().putString("last_foreground_package", packageName).apply()
                
                // If we just left the unlocked app, record the time we left it.
                if (previousApp == unlockedApp && previousApp.isNotEmpty()) {
                    prefs.edit().putLong("leave_time_$previousApp", System.currentTimeMillis()).apply()
                }
                
                if (lockedApps.contains(packageName)) {
                    var needsLocking = true
                    
                    if (unlockedApp == packageName) {
                        val leaveTime = prefs.getLong("leave_time_$packageName", System.currentTimeMillis())
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
                        
                        // For "Immediately", we want 0 grace period so it locks instantly.
                        val effectiveGracePeriod = autoLockTimeMs
                        
                        if (System.currentTimeMillis() - leaveTime < effectiveGracePeriod) {
                            needsLocking = false
                        }
                    }

                    val justUnlockedTime = prefs.getLong("just_unlocked_time_$packageName", 0L)
                    if (System.currentTimeMillis() - justUnlockedTime < 1000L) {
                        needsLocking = false // Just came from successfully unlocking this app (e.g., via notification)
                    }

                    if (needsLocking) {
                        Log.d("MLockService", "Launching LockScreen for $packageName")
                        val intent = Intent(this, LockScreenActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            putExtra("package_name", packageName)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private var screenOffReceiver: android.content.BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
        screenOffReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                if (intent.action == android.content.Intent.ACTION_SCREEN_OFF) {
                    val prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .remove("current_unlocked_app")
                        .remove("last_foreground_package")
                        .apply()
                    lastForegroundPackage = ""
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(this, screenOffReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        screenOffReceiver?.let { unregisterReceiver(it) }
    }

    override fun onInterrupt() {}
}
