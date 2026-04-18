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

    @Test
    fun rejectsGenerationsWithoutBackdropOrLogo() {
        val details = TmdbDetails(
            id = 1,
            title = "Movie",
            year = 2026,
            backdropUrl = "https://image.tmdb.org/t/p/original/backdrop.jpg",
            logoUrl = "https://image.tmdb.org/t/p/original/logo.png"
        )

        assertEquals(true, LocalWallpaperGenerator.isRenderable(details, hasBackdrop = true, hasLogo = true))
        assertEquals(false, LocalWallpaperGenerator.isRenderable(details, hasBackdrop = false, hasLogo = true))
        assertEquals(false, LocalWallpaperGenerator.isRenderable(details, hasBackdrop = true, hasLogo = false))
        assertEquals(false, LocalWallpaperGenerator.isRenderable(details.copy(logoUrl = null), hasBackdrop = true, hasLogo = true))
    }
}
