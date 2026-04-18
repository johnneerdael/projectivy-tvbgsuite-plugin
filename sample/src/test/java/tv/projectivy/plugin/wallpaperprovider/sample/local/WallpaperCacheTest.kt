package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperCacheTest {
    @Test
    fun safeFileNameRemovesDangerousCharacters() {
        assertEquals("Avatar Fire and Ash - 2025.jpg", WallpaperCache.safeFileName("Avatar: Fire/and Ash", 2025))
    }
}
