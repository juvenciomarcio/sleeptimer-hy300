package com.sleeptimer.hy300

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

class TimerService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_MINUTES = "minutes"
        private const val CHANNEL = "sleep_timer"

        /** Instante (elapsedRealtime) em que o timer termina; 0 = inativo. */
        @Volatile var endAt: Long = 0L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val finish = Runnable { onTimerFinished() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 30)
                val ms = minutes * 60_000L
                handler.removeCallbacks(finish)
                endAt = SystemClock.elapsedRealtime() + ms
                startForeground(1, buildNotification(minutes))
                handler.postDelayed(finish, ms)
            }
            else -> cancelTimer()
        }
        return START_NOT_STICKY
    }

    private fun cancelTimer() {
        handler.removeCallbacks(finish)
        endAt = 0L
        stopForeground(true)
        stopSelf()
    }

    private fun onTimerFinished() {
        pauseMedia()
        val shutdown = getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("shutdown", true)
        Thread {
            var done = false
            if (shutdown) {
                try { AdbHelper.shutdown(this); done = true } catch (_: Exception) { }
            }
            if (!done) lockScreen()   // alternativa: apagar a tela
            handler.post { cancelTimer() }
        }.start()
    }

    /** Pausa/para players que respondam às teclas de mídia (YouTube, Netflix, etc.). */
    private fun pauseMedia() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for (key in intArrayOf(KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP)) {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
        }
    }

    /** Apaga a tela: administrador de dispositivo (lockNow) ou, se houver root, tecla SLEEP. */
    private fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            return
        }
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 223")) // KEYCODE_SLEEP
        } catch (_: Exception) { /* sem root: nada a fazer */ }
    }

    private fun buildNotification(minutes: Int): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Sleep Timer", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL)
                else Notification.Builder(this)
        return b.setContentTitle("Sleep Timer ativo")
            .setContentText("Desliga a tela em $minutes min")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(finish)
        super.onDestroy()
    }
}
