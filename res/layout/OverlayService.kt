package com.teu.ffext

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.*
import com.teu.ffext.core.Config

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var painelView: View
    private lateinit var botaoView: View
    private lateinit var paramsPainel: WindowManager.LayoutParams
    private lateinit var paramsBotao: WindowManager.LayoutParams
    private lateinit var cfg: Config

    override fun onCreate() {
        super.onCreate()
        cfg = ConfigStore.load(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        criarPainel()
        criarBotaoFlutuante()
        // Inicia com o painel visível e o botão escondido
        botaoView.visibility = View.GONE
    }

    private fun criarPainel() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        painelView = inflater.inflate(R.layout.painel_flutuante, null)

        paramsPainel = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        paramsPainel.gravity = Gravity.TOP or Gravity.START
        paramsPainel.x = 100
        paramsPainel.y = 200

        windowManager.addView(painelView, paramsPainel)

        configurarArrastarPainel()
        configurarAbas()
        configurarToggles()
        
        // Botão fechar dentro do painel
        painelView.findViewById<View>(R.id.btn_fechar).setOnClickListener {
            esconderPainel()
        }
    }

    private fun criarBotaoFlutuante() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        botaoView = inflater.inflate(R.layout.botao_flutuante, null)

        paramsBotao = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        paramsBotao.gravity = Gravity.TOP or Gravity.START
        paramsBotao.x = 100
        paramsBotao.y = 100

        windowManager.addView(botaoView, paramsBotao)

        // Arrastar a bolinha
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        botaoView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = paramsBotao.x
                    initialY = paramsBotao.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    paramsBotao.x = initialX + (event.rawX - initialTouchX).toInt()
                    paramsBotao.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(botaoView, paramsBotao)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Se moveu muito pouco, é um clique (abre o painel)
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.sqrt((dx * dx + dy * dy).toDouble()) < 10) {
                        mostrarPainel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun esconderPainel() {
        painelView.visibility = View.GONE
        botaoView.visibility = View.VISIBLE
    }

    private fun mostrarPainel() {
        // Sincroniza a posição: o painel abre onde a bolinha tava
        paramsPainel.x = paramsBotao.x
        paramsPainel.y = paramsBotao.y
        windowManager.updateViewLayout(painelView, paramsPainel)
        
        botaoView.visibility = View.GONE
        painelView.visibility = View.VISIBLE
    }

    private fun configurarArrastarPainel() {
        val barraTitulo = painelView.findViewById<View>(R.id.barra_titulo)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        barraTitulo.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = paramsPainel.x
                    initialY = paramsPainel.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    paramsPainel.x = initialX + (event.rawX - initialTouchX).toInt()
                    paramsPainel.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(painelView, paramsPainel)
                    true
                }
                else -> false
            }
        }
    }

    private fun configurarAbas() {
        val pageAim = painelView.findViewById<View>(R.id.page_aim)
        val pageEsp = painelView.findViewById<View>(R.id.page_esp)
        val pageRage = painelView.findViewById<View>(R.id.page_rage)
        val pageCfg = painelView.findViewById<View>(R.id.page_cfg)

        fun togglePage(active: View, activeTab: View) {
            pageAim.visibility = View.GONE
            pageEsp.visibility = View.GONE
            pageRage.visibility = View.GONE
            pageCfg.visibility = View.GONE
            active.visibility = View.VISIBLE

            val tabs = listOf(R.id.tab_aim, R.id.tab_esp, R.id.tab_rage, R.id.tab_cfg)
            tabs.forEach { painelView.findViewById<View>(it).setBackgroundColor(0xFF222222.toInt()) }
            activeTab.setBackgroundColor(0xFFFF0000.toInt())
        }

        painelView.findViewById<View>(R.id.tab_aim).setOnClickListener { togglePage(pageAim, it) }
        painelView.findViewById<View>(R.id.tab_esp).setOnClickListener { togglePage(pageEsp, it) }
        painelView.findViewById<View>(R.id.tab_rage).setOnClickListener { togglePage(pageRage, it) }
        painelView.findViewById<View>(R.id.tab_cfg).setOnClickListener { togglePage(pageCfg, it) }
    }

    private fun configurarToggles() {
        painelView.findViewById<CheckBox>(R.id.cb_spin_bot).setOnCheckedChangeListener { _, isChecked ->
            cfg.spinEnabled = isChecked
            ConfigStore.save(this, cfg)
        }
        
        painelView.findViewById<CheckBox>(R.id.cb_esp_box).setOnCheckedChangeListener { _, isChecked ->
            cfg.espEnabled = isChecked
            ConfigStore.save(this, cfg)
        }
        
        // Adiciona os outros checkboxes aqui igual ao de cima
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::painelView.isInitialized) windowManager.removeView(painelView)
        if (::botaoView.isInitialized) windowManager.removeView(botaoView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
