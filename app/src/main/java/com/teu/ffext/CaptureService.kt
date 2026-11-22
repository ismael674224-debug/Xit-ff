package com.teu.ffext

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.teu.ffext.core.Aimbot
import com.teu.ffext.core.Config
import com.teu.ffext.core.Detector
import com.teu.ffext.core.SpinBot

class CaptureService : Service() {
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    
    private lateinit var cfg: Config
    private lateinit var detector: Detector
    private lateinit var aimbot: Aimbot
    private lateinit var spinBot: SpinBot
    private var frameCount = 0L
    
    // Para calcular FPS
    private var lastFpsTime = 0L
    private var fpsFrames = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cfg = ConfigStore.load(this)
        detector = Detector(cfg)
        aimbot = Aimbot(cfg)
        spinBot = SpinBot(cfg)

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        criarNotificacao()

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = projectionManager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        val w = (metrics.widthPixels * cfg.captureScale).toInt()
        val h = (metrics.heightPixels * cfg.captureScale).toInt()

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)

        virtualDisplay = projection.createVirtualDisplay(
            "DeltaCapture",
            w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            frameCount++
            
            if (frameCount % cfg.processEveryN == 0L) {
                val plane = image.planes[0]
                val bitmap = Bitmap.createBitmap(plane.rowStride / plane.pixelStride, image.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                
                // Lógica de detecção e mira
                val dets = detector.detectar(bitmap)
                val cx = bitmap.width / 2
                val cy = bitmap.height / 2
                
                val alvo = aimbot.escolherAlvo(dets, cx, cy, cx, cy)
                val delta = alvo?.let { aimbot.calcularDelta(it, cx, cy) }
                val spinDelta = spinBot.passo(cx, cy, dets, System.currentTimeMillis())

                // Atualiza o Overlay
                OverlayService.atualizar(dets, alvo, spinDelta ?: delta, bitmap.width, bitmap.height, cfg)
            }
            image.close()
        }, Handler(Looper.getMainLooper()))

        return START_STICKY
    }

    private fun criarNotificacao() {
        val channelId = "delta_cap"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Captura", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Delta External Ativo")
            .setContentText("Capturando tela e aplicando funções...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay.release()
        imageReader.close()
        projection.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
