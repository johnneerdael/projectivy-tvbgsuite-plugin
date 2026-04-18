package tv.projectivy.plugin.wallpaperprovider.sample.local

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperRenderSpecTest {
    @Test
    fun logoBoxMatchesRequestedTopLeftComposition() {
        val spec = WallpaperRenderSpec.default()
        assertRectEquals(RectF(380f, 250f, 1420f, 620f), spec.logoBox)
    }

    @Test
    fun coverRectBiasesBackdropToUpperRight() {
        val spec = WallpaperRenderSpec.default()
        val rect = spec.coverCropRect(sourceWidth = 1920, sourceHeight = 1080)
        assertRectEquals(RectF(520f, 0f, 4004.44f, 1960f), rect)
    }

    @Test
    fun bottomGradientCoversLauncherRows() {
        val spec = WallpaperRenderSpec.default()
        assertEquals(1320f, spec.bottomGradientStartY, 0.01f)
    }

    @Test
    fun containRectCentersLogoInsideBox() {
        val spec = WallpaperRenderSpec.default()
        val rect = spec.containRect(sourceWidth = 1000, sourceHeight = 300, box = spec.logoBox)
        assertRectEquals(RectF(380f, 279f, 1420f, 591f), rect)
    }

    private fun assertRectEquals(expected: RectF, actual: RectF) {
        assertEquals(expected.left, actual.left, 0.01f)
        assertEquals(expected.top, actual.top, 0.01f)
        assertEquals(expected.right, actual.right, 0.01f)
        assertEquals(expected.bottom, actual.bottom, 0.01f)
    }
}
