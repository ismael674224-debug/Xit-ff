package com.teu.ffext.core

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

data class Deteccao(
    val cabeca: Rect,
    val corpo: Rect?,
    val centro: Point,
    val distanciaPx: Double
)

class Detector(private val cfg: Config) {
    // Cores base (BGR) - ajusta conforme o jogo alvo
    private val headBgr = Scalar(30.0, 30.0, 220.0)
    private val bodyBgr = Scalar(220.0, 80.0, 30.0)
    private val tol = 45.0

    fun detectar(bitmap: Bitmap): List<Deteccao> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val cabecas = acharPorCor(mat, headBgr)
        val corpos = acharPorCor(mat, bodyBgr)

        return cabecas.map { h ->
            val corpo = corpos.minByOrNull { c ->
                val dx = (c.x + c.width/2) - (h.x + h.width/2)
                val dy = (c.y + c.height/2) - (h.y + h.height)
                dx*dx + dy*dy
            }?.takeIf { c ->
                val dx = kotlin.math.abs((c.x + c.width/2) - (h.x + h.width/2))
                dx < h.width * 2
            }
            Deteccao(
                cabeca = h,
                corpo = corpo,
                centro = Point((h.x + h.width/2).toDouble(), (h.y + h.height/2).toDouble()),
                distanciaPx = 0.0 // Distância real precisa de cálculo 3D, aqui fica 0
            )
        }
    }

    private fun acharPorCor(mat: Mat, alvo: Scalar): List<Rect> {
        val mask = Mat()
        Core.inRange(mat,
            Scalar(alvo.`val`[0]-tol, alvo.`val`[1]-tol, alvo.`val`[2]-tol),
            Scalar(alvo.`val`[0]+tol, alvo.`val`[1]+tol, alvo.`val`[2]+tol),
            mask)
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(mask, contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        return contours.filter { Imgproc.contourArea(it) > 150.0 }
            .map { Imgproc.boundingRect(it) }
    }
}
