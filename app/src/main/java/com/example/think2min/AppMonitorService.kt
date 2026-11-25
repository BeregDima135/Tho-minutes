package com.example.think2min

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppMonitorService : AccessibilityService() {

    companion object {
        var allowedPackageTemp: String? = null
        var allowedTimestamp: Long = 0L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName == this.packageName) return

            val prefs = getSharedPreferences("Think2MinPrefs", Context.MODE_PRIVATE)
            val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

            if (blockedApps.contains(packageName)) {
                val now = System.currentTimeMillis()
                // Если пользователь уже выждал 2 минуты и нажал "Зайти", даем 15 минут спокойного использования
                if (packageName == allowedPackageTemp && (now - allowedTimestamp) < 15 * 60 * 1000) {
                    return
                }

                val intent = Intent(this, BlockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("TARGET_PACKAGE", packageName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
