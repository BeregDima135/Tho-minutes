package com.example.think2min

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isBlocked: Boolean
)

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnEnableService: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnEnableService = findViewById(R.id.btnEnableService)
        val recyclerView = findViewById<RecyclerView>(R.id.appsRecyclerView)

        btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppsAdapter(loadInstalledApps())
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        if (isAccessibilityServiceEnabled()) {
            statusText.text = "Защита активна ✓"
            btnEnableService.text = "Настройки службы"
        } else {
            statusText.text = "Служба отслеживания выключена"
            btnEnableService.text = "Включить в настройках"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/${AppMonitorService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun loadInstalledApps(): List<AppItem> {
        val prefs = getSharedPreferences("Think2MinPrefs", Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedApps = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        val appList = mutableListOf<AppItem>()

        for (info in resolvedApps) {
            val pkg = info.activityInfo.packageName
            if (pkg == packageName) continue
            val label = info.loadLabel(pm).toString()
            val icon = info.loadIcon(pm)
            appList.add(AppItem(label, pkg, icon, blockedSet.contains(pkg)))
        }

        return appList.sortedBy { it.name.lowercase() }
    }

    inner class AppsAdapter(private val items: List<AppItem>) :
        RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.appIcon)
            val name: TextView = view.findViewById(R.id.appName)
            val switch: SwitchCompat = view.findViewById(R.id.appSwitch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.icon.setImageDrawable(item.icon)

            holder.switch.setOnCheckedChangeListener(null)
            holder.switch.isChecked = item.isBlocked

            holder.switch.setOnCheckedChangeListener { _, isChecked ->
                item.isBlocked = isChecked
                saveBlockedState(item.packageName, isChecked)
            }
        }

        override fun getItemCount() = items.size

        private fun saveBlockedState(pkg: String, blocked: Boolean) {
            val prefs = getSharedPreferences("Think2MinPrefs", Context.MODE_PRIVATE)
            val currentSet = prefs.getStringSet("blocked_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (blocked) {
                currentSet.add(pkg)
            } else {
                currentSet.remove(pkg)
            }
            prefs.edit().putStringSet("blocked_apps", currentSet).apply()
        }
    }
}
