package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbArtworkRepositoryTest {
    @Test
    fun selectsEnglishLogoBeforeOtherLogos() {
        val images = TmdbImagesResponse(
            logos = listOf(
                TmdbLogo(filePath = "/other.png", iso6391 = "fr"),
                TmdbLogo(filePath = "/english.png", iso6391 = "en")
            )
        )

        assertEquals("https://image.tmdb.org/t/p/original/english.png", TmdbArtworkRepository.selectLogoUrl(images))
    }

    @Test
    fun buildsBackdropUrl() {
        assertEquals(
            "https://image.tmdb.org/t/p/original/backdrop.jpg",
            TmdbArtworkRepository.imageUrl("/backdrop.jpg")
        )
    }
}
