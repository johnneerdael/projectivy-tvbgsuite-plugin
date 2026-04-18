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

    @Test
    fun mapsTrendingAndListItemsToCandidates() {
        val media = TraktMediaSummary(
            title = "Project Hail Mary",
            year = 2026,
            ids = TraktIds(trakt = 1003596, slug = "project-hail-mary-2026", imdb = "tt12042730", tmdb = 490205)
        )

        assertEquals(
            WallpaperCandidate(
                catalog = CatalogKind.TRENDING_MOVIE,
                title = "Project Hail Mary",
                year = 2026,
                tmdbId = 490205,
                imdbId = "tt12042730",
                traktId = 1003596,
                tvdbId = null
            ),
            TraktTrendingItem(watchers = 42, movie = media).toWallpaperCandidate(CatalogKind.TRENDING_MOVIE)
        )

        assertEquals(
            WallpaperCandidate(
                catalog = CatalogKind.POPULAR_LIST_MOVIE,
                title = "Project Hail Mary",
                year = 2026,
                tmdbId = 490205,
                imdbId = "tt12042730",
                traktId = 1003596,
                tvdbId = null
            ),
            TraktListItem(type = "movie", movie = media).toWallpaperCandidate(CatalogKind.POPULAR_LIST_MOVIE)
        )
    }

    @Test
    fun mapsPopularListsToSelectableOptions() {
        val item = TraktPopularListItem(
            likeCount = 109,
            commentCount = 20,
            list = TraktListSummary(
                name = "Top Chihuahua Movies",
                itemCount = 50,
                ids = TraktListIds(trakt = 1338, slug = "top-chihuahua-movies"),
                user = TraktUserSummary(username = "ignored", ids = TraktUserIds(slug = "list-owner"))
            ),
            user = TraktUserSummary(username = "Justin", ids = TraktUserIds(slug = "justin"))
        )

        assertEquals(
            TraktPopularListOption(
                key = "popular_list:justin:top-chihuahua-movies",
                title = "Top Chihuahua Movies",
                userId = "justin",
                listId = "top-chihuahua-movies",
                itemCount = 50
            ),
            item.toOption()
        )
    }
}
