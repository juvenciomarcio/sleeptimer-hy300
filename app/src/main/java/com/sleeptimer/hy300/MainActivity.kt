package com.sleeptimer.hy300

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Interface 100% em código, com botões grandes e navegação por D-pad (controle do projetor). */
class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var countdown: TextView
    private lateinit var status: TextView
    private lateinit var adminBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var modeBtn: Button

    private fun updateModeLabel() {
        val on = getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("shutdown", true)
        modeBtn.text = if (on) "Ao final: DESLIGAR (ADB)" else "Ao final: só apagar tela"
    }

    /** Conecta ao ADB local; na primeira vez o projetor mostra "Permitir depuração" — marque "Sempre". */
    private fun authorizeAdb() {
        Toast.makeText(this, "Conectando ao ADB... aceite o aviso na tela", Toast.LENGTH_LONG).show()
        Thread {
            val msg = try { AdbHelper.shell(this, "echo ok"); "ADB autorizado ✔" }
                      catch (e: Exception) { "Falha no ADB: ${e.message}" }
            runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }.start()
    }

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B0F1A"))
            setPadding(dp(48), dp(32), dp(48), dp(32))
        }

        root.addView(text("Sleep Timer", 34f, Color.WHITE))
        countdown = text("--:--:--", 64f, Color.parseColor("#7CC4FF"))
        root.addView(countdown)
        status = text("", 16f, Color.LTGRAY)
        root.addView(status)

        val presets = intArrayOf(15, 30, 45, 60, 90, 120)
        for (rowMinutes in presets.toList().chunked(3)) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (m in rowMinutes) {
                row.addView(button("$m min") { start(m) })
            }
            root.addView(row)
        }

        cancelBtn = button("Cancelar timer") { stop() }
        root.addView(cancelBtn)
        adminBtn = button("Ativar desligamento de tela") { requestAdmin() }
        root.addView(adminBtn)

        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        modeBtn = button("") { 
            prefs.edit().putBoolean("shutdown", !prefs.getBoolean("shutdown", true)).apply()
            updateModeLabel()
        }
        row2.addView(modeBtn)
        row2.addView(button("Autorizar ADB (1ª vez)") { authorizeAdb() })
        root.addView(row2)
        updateModeLabel()

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun start(minutes: Int) {
        val i = Intent(this, TimerService::class.java)
            .setAction(TimerService.ACTION_START)
            .putExtra(TimerService.EXTRA_MINUTES, minutes)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        handler.postDelayed({ refresh() }, 300)
    }

    private fun stop() {
        startService(Intent(this, TimerService::class.java).setAction(TimerService.ACTION_STOP))
        TimerService.endAt = 0L
        refresh()
    }

    private fun isAdmin(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))
    }

    private fun requestAdmin() {
        val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this, AdminReceiver::class.java)
            )
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Necessário para apagar a tela quando o timer terminar."
            )
        startActivity(i)
    }

    private fun refresh() {
        val left = TimerService.endAt - SystemClock.elapsedRealtime()
        if (TimerService.endAt > 0 && left > 0) {
            val s = left / 1000
            countdown.text = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
            status.text = "Timer ativo — a tela será desligada ao final."
            cancelBtn.visibility = View.VISIBLE
        } else {
            countdown.text = "--:--:--"
            status.text = "Escolha uma duração."
            cancelBtn.visibility = View.GONE
        }
        adminBtn.visibility = if (isAdmin()) View.GONE else View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(t: String, size: Float, color: Int) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 20f
        isFocusable = true
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(8), dp(8), dp(8), dp(8)) }
        minWidth = dp(160)
        setOnClickListener { onClick() }
    }
}
