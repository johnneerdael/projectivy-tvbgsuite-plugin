package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import java.io.File
import java.io.FileOutputStream

class WallpaperRenderer(private val spec: WallpaperRenderSpec = WallpaperRenderSpec.default()) {
    fun render(
        backdrop: Bitmap,
        logo: Bitmap?,
        outputFile: File
    ) {
        val output = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawColor(Color.BLACK)
        drawDarkBackdropUnderlay(canvas, backdrop, paint)
        val clippedBackdrop = canvas.save()
        canvas.clipRect(spec.backdropBox)
        canvas.drawBitmap(backdrop, Rect(0, 0, backdrop.width, backdrop.height), spec.coverCropRect(backdrop.width, backdrop.height), paint)
        canvas.restoreToCount(clippedBackdrop)
        drawLeftGradient(canvas)
        drawBottomGradient(canvas)

        if (logo != null) {
            canvas.drawBitmap(logo, Rect(0, 0, logo.width, logo.height), spec.containRect(logo.width, logo.height, spec.logoBox), paint)
        }

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { stream ->
            output.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        }
        output.recycle()
    }

    private fun drawDarkBackdropUnderlay(canvas: Canvas, backdrop: Bitmap, paint: Paint) {
        paint.alpha = 90
        canvas.drawBitmap(backdrop, Rect(0, 0, backdrop.width, backdrop.height), spec.fullCanvasCoverRect(backdrop.width, backdrop.height), paint)
        paint.alpha = 255
        canvas.drawColor(Color.argb(135, 0, 0, 0))
    }

    private fun drawLeftGradient(canvas: Canvas) {
        canvas.drawRect(0f, 0f, spec.leftEdgeMaskStartX, spec.height.toFloat(), Paint().apply {
            color = Color.BLACK
        })

        val opaqueStop = ((spec.leftGradientOpaqueUntilX - spec.leftEdgeMaskStartX) /
            (spec.leftGradientEndX - spec.leftEdgeMaskStartX)).coerceIn(0f, 1f)
        val paint = Paint()
        paint.shader = LinearGradient(
            spec.leftEdgeMaskStartX, 0f, spec.leftGradientEndX, 0f,
            intArrayOf(Color.BLACK, Color.BLACK, Color.argb(210, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, opaqueStop, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(spec.leftEdgeMaskStartX, 0f, spec.leftGradientEndX, spec.height.toFloat(), paint)
    }

    private fun drawBottomGradient(canvas: Canvas) {
        val strongStop = ((spec.bottomGradientStrongUntilY - spec.bottomGradientStartY) /
            (spec.bottomEdgeMaskEndY - spec.bottomGradientStartY)).coerceIn(0f, 1f)
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, spec.bottomGradientStartY, 0f, spec.bottomEdgeMaskEndY,
            intArrayOf(Color.TRANSPARENT, Color.argb(220, 0, 0, 0), Color.BLACK),
            floatArrayOf(0f, strongStop, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, spec.bottomGradientStartY, spec.width.toFloat(), spec.bottomEdgeMaskEndY, paint)
        canvas.drawRect(0f, spec.bottomEdgeMaskEndY, spec.width.toFloat(), spec.height.toFloat(), Paint().apply {
            color = Color.BLACK
        })
    }
}
