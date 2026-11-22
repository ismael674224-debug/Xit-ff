package com.teu.ffext.input

import rikka.shizuku.Shizuku
import kotlin.math.*

class TouchInjector {
    private var process: Process? = null
    private var writer: java.io.BufferedWriter? = null

    fun disponivel(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun iniciar(): Boolean {
        if (!disponivel()) return false
        return try {
            process = Shizuku.newProcess(arrayOf("sh"), null, null)
            writer = process!!.outputStream.bufferedWriter()
            true
        } catch (e: Exception) { false }
    }

    fun encerrar() {
        try { writer?.close(); process?.destroy() } catch (_: Exception) {}
        writer = null; process = null
    }

    fun mover(dx: Float, dy: Float, duracaoMs: Int = 8) {
        val w = writer ?: return
        val cx = 540; val cy = 1200  // Ajuste pro centro do teu device
        val x1 = cx; val y1 = cy
        val x2 = (cx + dx).toInt().coerceIn(0, 1080)
        val y2 = (cy + dy).toInt().coerceIn(0, 2400)
        w.write("input swipe $x1 $y1 $x2 $y2 $duracaoMs\n")
        w.flush()
    }

    fun tap(x: Int, y: Int) {
        val w = writer ?: return
        w.write("input tap $x $y\n")
        w.flush()
    }
}
