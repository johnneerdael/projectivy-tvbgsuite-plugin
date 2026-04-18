package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalWallpaperGeneratorTest {
    @Test
    fun alternatesMovieAndShowCandidatesByDefault() {
        val movies = listOf(
            WallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE, "Movie A", 2026, 1, "tt1", 1, null),
            WallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE, "Movie B", 2026, 2, "tt2", 2, null)
        )
        val shows = listOf(
            WallpaperCandidate(CatalogKind.ANTICIPATED_SHOW, "Show A", 2026, 3, "tt3", 3, 4)
        )

        assertEquals(listOf("Movie A", "Show A", "Movie B"), LocalWallpaperGenerator.interleave(movies, shows).map { it.title })
    }
}
