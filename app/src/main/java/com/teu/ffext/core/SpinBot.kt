package com.teu.ffext.core

import kotlin.math.*

class SpinBot(private val cfg: Config) {
    private var angulo = 0f
    private var ativo = false
    private var ultimoPasso = 0L

    fun ligar() { ativo = true; angulo = 0f }
    fun desligar() { ativo = false }

    fun passo(
        telaCx: Int, telaCy: Int,
        alvos: List<Deteccao>,
        agora: Long
    ): Pair<Float, Float>? {
        if (!cfg.spinEnabled || !ativo) return null
        if (agora - ultimoPasso < cfg.spinIntervalMs) return null
        ultimoPasso = agora

        if (cfg.spinMode == SpinMode.ON_TARGET && alvos.isEmpty()) return null

        val raio = cfg.spinRadius
        val rad = Math.toRadians(angulo.toDouble())

        if (cfg.spinMode == SpinMode.SNAP_SPIN && alvos.isNotEmpty()) {
            val alvo = alvos.minByOrNull { d ->
                val dx = d.centro.x - telaCx; val dy = d.centro.y - telaCy
                dx*dx + dy*dy
            }
            if (alvo != null) {
                val dxAlvo = alvo.centro.x - telaCx
                val dyAlvo = alvo.centro.y - telaCy
                val angAlvo = Math.toDegrees(atan2(dyAlvo, dxAlvo)).toFloat()
                val diff = ((angAlvo - angulo + 540f) % 360f) - 180f
                if (abs(diff) < cfg.spinSnapAngle) {
                    return (dxAlvo * cfg.spinSnapStrength).toFloat() to
                           (dyAlvo * cfg.spinSnapStrength).toFloat()
                }
            }
        }

        val nx = (raio * cos(rad)).toFloat()
        val ny = (raio * sin(rad)).toFloat()
        angulo += cfg.spinStepGraus
        if (angulo >= 360f) angulo -= 360f

        val raioJitter = if (cfg.humanize) (Math.random().toFloat() - 0.5f) * cfg.spinJitter else 0f
        return (nx + raioJitter) to (ny + raioJitter)
    }
}
