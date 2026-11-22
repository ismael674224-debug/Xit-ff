package com.teu.ffext.core

import kotlin.math.*
import kotlin.random.Random

class Aimbot(private val cfg: Config) {
    private var proximoReacao = 0L
    private var overshoot = 0f

    fun escolherAlvo(
        dets: List<Deteccao>,
        telaCx: Int, telaCy: Int,
        cursorX: Int, cursorY: Int
    ): Deteccao? {
        if (!cfg.aimEnabled || cfg.aimMode == AimMode.OFF) return null
        if (dets.isEmpty()) return null

        val filtrados = dets.filter { d ->
            val dx = d.centro.x - telaCx
            val dy = d.centro.y - telaCy
            when (cfg.aimMode) {
                AimMode.R360 -> true
                AimMode.NORMAL -> sqrt(dx*dx + dy*dy) <= cfg.fovRadius
                else -> false
            }
        }
        if (filtrados.isEmpty()) return null

        return when (cfg.priority) {
            Priority.CENTER -> filtrados.minByOrNull {
                val dx = it.centro.x - telaCx; val dy = it.centro.y - telaCy
                dx*dx + dy*dy
            }
            Priority.CURSOR -> filtrados.minByOrNull {
                val dx = it.centro.x - cursorX; val dy = it.centro.y - cursorY
                dx*dx + dy*dy
            }
            Priority.NEAREST -> filtrados.minByOrNull { it.distanciaPx }
        }
    }

    fun calcularDelta(alvo: Deteccao, telaCx: Int, telaCy: Int): Pair<Float, Float>? {
        val now = System.currentTimeMillis()
        if (now < proximoReacao) return null

        val tx = when (cfg.aimTarget) {
            AimTarget.HEAD -> alvo.cabeca.let { (it.x + it.width/2).toFloat() }
            AimTarget.NECK -> alvo.cabeca.let { (it.x + it.width/2).toFloat() }
            AimTarget.BODY -> alvo.corpo?.let { (it.x + it.width/2).toFloat() }
                ?: alvo.cabeca.let { (it.x + it.width/2).toFloat() }
        }
        val ty = when (cfg.aimTarget) {
            AimTarget.HEAD -> alvo.cabeca.let { (it.y + it.height/2).toFloat() }
            AimTarget.NECK -> alvo.cabeca.let { (it.y + it.height * 0.9f).toFloat() }
            AimTarget.BODY -> alvo.corpo?.let { (it.y + it.height * 0.3f).toFloat() }
                ?: alvo.cabeca.let { (it.y + it.height/2).toFloat() }
        }

        var dx = tx - telaCx
        var dy = ty - telaCy

        val dist = sqrt(dx*dx + dy*dy).coerceAtLeast(1f)
        val accel = (dist / 500f).coerceIn(0.05f, 0.9f)
        val s = cfg.smooth * accel

        if (cfg.humanize) {
            if (overshoot == 0f && Random.nextFloat() < 0.15f) {
                overshoot = Random.nextFloat() * 0.08f + 0.02f
            }
            val o = 1f + overshoot
            dx *= o; dy *= o
            overshoot *= 0.7f
            if (overshoot < 0.001f) overshoot = 0f
        }

        if (cfg.gaussError > 0f) {
            dx += gauss() * cfg.gaussError
            dy += gauss() * cfg.gaussError
        }

        if (cfg.humanize) {
            dx += (Random.nextFloat() - 0.5f) * 0.8f
            dy += (Random.nextFloat() - 0.5f) * 0.8f
        }

        dx *= s; dy *= s

        if (cfg.reactionMs > 0) {
            proximoReacao = now + Random.nextLong(
                (cfg.reactionMs * 0.6).toLong(),
                cfg.reactionMs.toLong() + 1
            )
        }
        return dx to dy
    }

    private fun gauss(): Float {
        val u1 = Random.nextFloat().coerceAtLeast(1e-6f)
        val u2 = Random.nextFloat()
        return sqrt(-2f * ln(u1)) * cos(2f * PI.toFloat() * u2)
    }
}
