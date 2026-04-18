package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class AnticipatedCatalogRepositoryTest {
    @Test
    fun mapsMovieAndShowItemsToCandidates() {
        val movie = TraktAnticipatedItem(
            listCount = 5362,
            movie = TraktMediaSummary(
                title = "The Martian",
                year = 2015,
                ids = TraktIds(trakt = 183371, slug = "the-martian-2015", imdb = "tt3659388", tmdb = 286217, tvdb = null)
            ),
            show = null
        )
        val show = TraktAnticipatedItem(
            listCount = 5383,
            movie = null,
            show = TraktMediaSummary(
                title = "Supergirl",
                year = 2015,
                ids = TraktIds(trakt = 99046, slug = "supergirl", imdb = "tt4016454", tmdb = 62688, tvdb = 295759)
            )
        )

        assertEquals(
            WallpaperCandidate(
                catalog = CatalogKind.ANTICIPATED_MOVIE,
                title = "The Martian",
                year = 2015,
                tmdbId = 286217,
                imdbId = "tt3659388",
                traktId = 183371,
                tvdbId = null
            ),
            movie.toWallpaperCandidate(CatalogKind.ANTICIPATED_MOVIE)
        )
        assertEquals(
            WallpaperCandidate(
                catalog = CatalogKind.ANTICIPATED_SHOW,
                title = "Supergirl",
                year = 2015,
                tmdbId = 62688,
                imdbId = "tt4016454",
                traktId = 99046,
                tvdbId = 295759
            ),
            show.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW)
        )
    }

    @Test
    fun ignoresItemsWithoutTmdbId() {
        val show = TraktAnticipatedItem(
            listCount = 1481,
            movie = null,
            show = TraktMediaSummary(
                title = "The Expanse",
                year = 2015,
                ids = TraktIds(trakt = 77199, slug = "the-expanse", imdb = "tt3230854", tmdb = null, tvdb = 280619)
            )
        )

        assertEquals(null, show.toWallpaperCandidate(CatalogKind.ANTICIPATED_SHOW))
    }
}
