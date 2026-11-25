package com.sleeptimer.hy300

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

/** Fala com o ADB do próprio projetor (127.0.0.1:5555) para ter permissão de desligar. */
object AdbHelper {
    private const val PORT = 5555

    private fun keyPair(ctx: Context): AdbKeyPair {
        val priv = File(ctx.filesDir, "adbkey")
        val pub = File(ctx.filesDir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }

    /** Executa um comando shell como usuário "shell". Lança exceção se falhar. Não use na thread principal. */
    fun shell(ctx: Context, cmd: String): String {
        Dadb.create("127.0.0.1", PORT, keyPair(ctx)).use { adb ->
            return adb.shell(cmd).allOutput
        }
    }

    fun shutdown(ctx: Context) {
        try {
            shell(ctx, "svc power shutdown")
        } catch (e: Exception) {
            shell(ctx, "reboot -p")
        }
    }
}
