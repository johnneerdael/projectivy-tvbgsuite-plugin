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
        canvas.drawBitmap(backdrop, Rect(0, 0, backdrop.width, backdrop.height), spec.coverCropRect(backdrop.width, backdrop.height), paint)
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

    private fun drawLeftGradient(canvas: Canvas) {
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, 0f, spec.leftGradientEndX, 0f,
            intArrayOf(Color.argb(230, 10, 4, 0), Color.argb(160, 20, 8, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, spec.leftGradientEndX, spec.height.toFloat(), paint)
    }

    private fun drawBottomGradient(canvas: Canvas) {
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, spec.bottomGradientStartY, 0f, spec.height.toFloat(),
            Color.TRANSPARENT,
            Color.argb(245, 8, 4, 0),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, spec.bottomGradientStartY, spec.width.toFloat(), spec.height.toFloat(), paint)
    }
}
