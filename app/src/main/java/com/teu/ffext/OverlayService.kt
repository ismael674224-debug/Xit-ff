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
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var cfg: Config

    override fun onCreate() {
        super.onCreate()
        cfg = ConfigStore.load(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        criarPainel()
    }

    private fun criarPainel() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        painelView = inflater.inflate(R.layout.painel_flutuante, null)

        // Configurações da janela flutuante (Draggable)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        windowManager.addView(painelView, params)

        configurarArrastar()
        configurarAbas()
        configurarToggles()
    }

    private fun configurarArrastar() {
        val barraTitulo = painelView.findViewById<View>(R.id.barra_titulo)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        barraTitulo.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(painelView, params)
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

            // reset das cores das abas
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
        // Aqui tu liga os checkboxes do XML com as variáveis do Config
        painelView.findViewById<CheckBox>(R.id.cb_spin_bot).setOnCheckedChangeListener { _, isChecked ->
            cfg.spinEnabled = isChecked
            ConfigStore.save(this, cfg)
        }
        
        painelView.findViewById<CheckBox>(R.id.cb_esp_box).setOnCheckedChangeListener { _, isChecked ->
            cfg.espEnabled = isChecked
            ConfigStore.save(this, cfg)
        }
        
        // Botão Fechar (esconde o painel mas não para o service)
        painelView.findViewById<View>(R.id.btn_fechar).setOnClickListener {
            painelView.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::painelView.isInitialized) windowManager.removeView(painelView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
