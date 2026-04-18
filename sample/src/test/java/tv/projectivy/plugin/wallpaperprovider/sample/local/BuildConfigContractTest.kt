package tv.projectivy.plugin.wallpaperprovider.sample.local

import com.traktlistbackdrops.tv.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BuildConfigContractTest {
    @Test
    fun providerBuildConfigFieldsExist() {
        assertNotNull(BuildConfig.TMDB_API_KEY)
        assertEquals("https://api.themoviedb.org/3/", BuildConfig.TMDB_API_URL.ifBlank { "https://api.themoviedb.org/3/" })
        assertNotNull(BuildConfig.TRAKT_CLIENT_ID)
        assertNotNull(BuildConfig.TRAKT_CLIENT_SECRET)
        assertEquals("https://api.trakt.tv/", BuildConfig.TRAKT_API_URL.ifBlank { "https://api.trakt.tv/" })
        assertEquals("urn:ietf:wg:oauth:2.0:oob", BuildConfig.TRAKT_REDIRECT_URI.ifBlank { "urn:ietf:wg:oauth:2.0:oob" })
    }
}
