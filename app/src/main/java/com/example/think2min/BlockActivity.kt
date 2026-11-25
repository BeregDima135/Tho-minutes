package com.example.think2min

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class BlockActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        targetPackage = intent.getStringExtra("TARGET_PACKAGE")

        val timerText = findViewById<TextView>(R.id.timerText)
        val subtitleHint = findViewById<TextView>(R.id.subtitleHint)
        val btnProceed = findViewById<Button>(R.id.btnProceed)
        val btnChangedMind = findViewById<Button>(R.id.btnChangedMind)

        // Запрещаем выход системной кнопкой "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ничего не делаем, выйти можно только нажав "Я передумал"
            }
        })

        // Кнопка "Я передумал" -> сворачиваем всё на главный экран телефона
        btnChangedMind.setOnClickListener {
            timer?.cancel()
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }

        // Кнопка "Зайти в приложение" (появится через 2 минуты)
        btnProceed.setOnClickListener {
            AppMonitorService.allowedPackageTemp = targetPackage
            AppMonitorService.allowedTimestamp = System.currentTimeMillis()
            targetPackage?.let { pkg ->
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            }
            finish()
        }

        // Таймер ровно на 2 минуты (120 000 миллисекунд)
        timer = object : CountDownTimer(120_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                timerText.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timerText.text = "00:00"
                subtitleHint.text = "2 минуты прошли. Ты всё ещё хочешь зайти?"
                btnProceed.visibility = View.VISIBLE
            }
        }.start()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
