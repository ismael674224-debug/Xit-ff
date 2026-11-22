companion object {
    var instance: OverlayService? = null
    // Adiciona essas variáveis
    private var lastFpsTime = 0L
    private var fpsFrames = 0
    var currentFps = 0

    fun atualizar(dets: List<Deteccao>, alvo: Deteccao?, delta: Pair<Float, Float>?, w: Int, h: Int, cfg: Config) {
        // Calcula o FPS
        fpsFrames++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            currentFps = fpsFrames
            fpsFrames = 0
            lastFpsTime = now
        }
        
        instance?.let { 
            it.dets = dets; it.alvo = alvo; it.delta = delta
            it.sw = w; it.sh = h; it.cfg = cfg; it.view.invalidate() 
        }
    }
}
