package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.graphics.RectF
import kotlin.math.max

data class WallpaperRenderSpec(
    val width: Int,
    val height: Int,
    val logoBox: RectF,
    val backdropBox: RectF,
    val leftGradientEndX: Float,
    val bottomGradientStartY: Float,
    val backdropBiasX: Float,
    val backdropBiasY: Float
) {
    fun coverCropRect(sourceWidth: Int, sourceHeight: Int): RectF {
        val boxWidth = backdropBox.right - backdropBox.left
        val boxHeight = backdropBox.bottom - backdropBox.top
        val scale = max(boxWidth / sourceWidth.toFloat(), boxHeight / sourceHeight.toFloat())
        val scaledW = sourceWidth * scale
        val scaledH = sourceHeight * scale
        val overflowX = scaledW - boxWidth
        val overflowY = scaledH - boxHeight
        val left = backdropBox.left - overflowX * backdropBiasX
        val top = backdropBox.top - overflowY * backdropBiasY
        return RectF(left, top, left + scaledW, top + scaledH)
    }

    fun containRect(sourceWidth: Int, sourceHeight: Int, box: RectF): RectF {
        val boxWidth = box.right - box.left
        val boxHeight = box.bottom - box.top
        val scale = minOf(boxWidth / sourceWidth.toFloat(), boxHeight / sourceHeight.toFloat())
        val scaledW = sourceWidth * scale
        val scaledH = sourceHeight * scale
        val left = box.left + (boxWidth - scaledW) / 2f
        val top = box.top + (boxHeight - scaledH) / 2f
        return RectF(left, top, left + scaledW, top + scaledH)
    }

    companion object {
        fun default(): WallpaperRenderSpec = WallpaperRenderSpec(
            width = 3840,
            height = 2160,
            logoBox = RectF(380f, 250f, 1420f, 620f),
            backdropBox = RectF(520f, 0f, 3840f, 1960f),
            leftGradientEndX = 2100f,
            bottomGradientStartY = 1320f,
            backdropBiasX = 0.72f,
            backdropBiasY = 0.18f
        )
    }
}
