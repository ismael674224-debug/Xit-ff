package com.teu.ffext

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*
import android.widget.*
import com.teu.ffext.core.Config
import com.teu.ffext.core.Deteccao

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var painelView: View
    private lateinit var botaoView: View
    private lateinit var paramsPainel: WindowManager.LayoutParams
    private lateinit var paramsBotao: WindowManager.LayoutParams
    private lateinit var cfg: Config
    private lateinit var espView: View // Nova View pra desenhar a ESP

    private var dets: List<Deteccao> = emptyList()
    private var alvo: Deteccao? = null
    private var delta: Pair<Float, Float>? = null
    private var sw = 0; private var sh = 0
    
    // FPS
    private var lastFpsTime = 0L
    private var fpsFrames = 0
    private var currentFps = 0

    override fun onCreate() {
        super.onCreate()
        cfg = ConfigStore.load(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        criarEspOverlay()
        criarPainel()
        criarBotaoFlutuante()
        botaoView.visibility = View.GONE
    }

    private fun criarEspOverlay() {
        espView = object : View(this) {
            val paintBox = Paint().apply { style = Paint.Style.STROKE }
            val paintLine = Paint().apply { style = Paint.Style.STROKE }
            val paintText = Paint().apply { textSize = 30f; color = Color.WHITE }
            val paintFov = Paint().apply { style = Paint.Style.STROKE; color = Color.argb(120, 0, 200, 255) }

            override fun onDraw(canvas: Canvas) {
                // Calcula FPS
                fpsFrames++
                val now = System.currentTimeMillis()
                if (now - lastFpsTime >= 1000) {
                    currentFps = fpsFrames; fpsFrames = 0; lastFpsTime = now
                }

                if (cfg.showFps) {
                    val paintFps = Paint().apply { color = Color.GREEN; textSize = 40f }
                    canvas.drawText("FPS: $currentFps", 50f, 80f, paintFps)
                }

                if (!cfg.espEnabled) return
                paintBox.color = Color.argb(cfg.espOpacity, 0, 255, 80)
                paintBox.strokeWidth = cfg.espThickness
                paintLine.color = Color.argb(cfg.espOpacity, 255, 200, 0)
                paintLine.strokeWidth = cfg.espThickness

                dets.forEach { d ->
                    val r = when (cfg.espBox) {
                        com.teu.ffext.core.EspBox.HEAD -> d.cabeca
                        com.teu.ffext.core.EspBox.BODY -> d.corpo ?: d.cabeca
                        com.teu.ffext.core.EspBox.FULL -> unir(d.cabeca, d.corpo)
                    }
                    canvas.drawRect(r.x.toFloat(), r.y.toFloat(),
                        (r.x + r.width).toFloat(), (r.y + r.height).toFloat(), paintBox)
                        
                    if (cfg.espLine) {
                        canvas.drawLine((r.x + r.width/2).toFloat(), r.y.toFloat(),
                            (sw/2).toFloat(), sh.toFloat(), paintLine)
                    }
                    if (cfg.espInfo) {
                        canvas.drawText("${d.distanciaPx.toInt()}m", r.x.toFloat(), r.y - 6f, paintText)
                    }
                }

                if (cfg.espFovCircle) {
                    canvas.drawCircle((sw/2).toFloat(), (sh/2).toFloat(), cfg.fovRadius.toFloat(), paintFov)
                }

                alvo?.let {
                    val p = Paint().apply { color = Color.RED; strokeWidth = 4f }
                    canvas.drawCircle(it.centro.x.toFloat(), it.centro.y.toFloat(), 8f, p)
                }
            }
        }
        
        val paramsEsp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(espView, paramsEsp)
    }

    private fun unir(a: Rect, b: Rect?): Rect {
        b ?: return a
        return Rect(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x + a.width, b.x + b.width), maxOf(a.y + a.height, b.y + b.height))
    }

    private fun criarPainel() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        painelView = inflater.inflate(R.layout.painel_flutuante, null)

        paramsPainel = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        )
        paramsPainel.gravity = Gravity.TOP or Gravity.START
        paramsPainel.x = 100; paramsPainel.y = 200
        windowManager.addView(painelView, paramsPainel)

        configurarArrastar(painelView, paramsPainel, painelView.findViewById(R.id.barra_titulo))
        configurarAbas()
        configurarToggles()
        
        painelView.findViewById<View>(R.id.btn_fechar).setOnClickListener { esconderPainel() }
    }

    private fun criarBotaoFlutuante() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        botaoView = inflater.inflate(R.layout.botao_flutuante, null)

        paramsBotao = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        )
        paramsBotao.gravity = Gravity.TOP or Gravity.START
        paramsBotao.x = 100; paramsBotao.y = 100
        windowManager.addView(botaoView, paramsBotao)

        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        botaoView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = paramsBotao.x; initialY = paramsBotao.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    paramsBotao.x = initialX + (event.rawX - initialTouchX).toInt()
                    paramsBotao.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(botaoView, paramsBotao)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX; val dy = event.rawY - initialTouchY
                    if (Math.sqrt((dx * dx + dy * dy).toDouble()) < 10) { mostrarPainel() }
                    true
                }
                else -> false
            }
        }
    }

    private fun esconderPainel() { painelView.visibility = View.GONE; botaoView.visibility = View.VISIBLE }
    private fun mostrarPainel() {
        paramsPainel.x = paramsBotao.x; paramsPainel.y = paramsBotao.y
        windowManager.updateViewLayout(painelView, paramsPainel)
        botaoView.visibility = View.GONE; painelView.visibility = View.VISIBLE
    }

    private fun configurarArrastar(view: View, params: WindowManager.LayoutParams, handle: View) {
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun configurarAbas() {
        val pages = listOf(R.id.page_aim, R.id.page_esp, R.id.page_rage, R.id.page_cfg).map { painelView.findViewById<View>(it) }
        val tabs = listOf(R.id.tab_aim, R.id.tab_esp, R.id.tab_rage, R.id.tab_cfg).map { painelView.findViewById<View>(it) }

        fun togglePage(active: View, activeTab: View) {
            pages.forEach { it.visibility = View.GONE }; active.visibility = View.VISIBLE
            tabs.forEach { it.setBackgroundColor(0xFF222222.toInt()) }; activeTab.setBackgroundColor(0xFFFF0000.toInt())
        }

        tabs[0].setOnClickListener { togglePage(pages[0], it) }
        tabs[1].setOnClickListener { togglePage(pages[1], it) }
        tabs[2].setOnClickListener { togglePage(pages[2], it) }
        tabs[3].setOnClickListener { togglePage(pages[3], it) }
    }

    private fun configurarToggles() {
        painelView.findViewById<CheckBox>(R.id.cb_spin_bot)?.setOnCheckedChangeListener { _, isChecked ->
            cfg.spinEnabled = isChecked; ConfigStore.save(this, cfg)
        }
        painelView.findViewById<CheckBox>(R.id.cb_esp_box)?.setOnCheckedChangeListener { _, isChecked ->
            cfg.espEnabled = isChecked; ConfigStore.save(this, cfg)
        }
    }

    companion object {
        var instance: OverlayService? = null
        fun atualizar(dets: List<Deteccao>, alvo: Deteccao?, delta: Pair<Float, Float>?, w: Int, h: Int, cfg: Config) {
            instance?.let { 
                it.dets = dets; it.alvo = alvo; it.delta = delta
                it.sw = w; it.sh = h; it.cfg = cfg; it.espView.invalidate() 
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::painelView.isInitialized) windowManager.removeView(painelView)
        if (::botaoView.isInitialized) windowManager.removeView(botaoView)
        if (::espView.isInitialized) windowManager.removeView(espView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
