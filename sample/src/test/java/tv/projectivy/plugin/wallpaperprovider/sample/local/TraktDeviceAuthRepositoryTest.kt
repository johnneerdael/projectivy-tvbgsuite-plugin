package tv.projectivy.plugin.wallpaperprovider.sample.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TraktDeviceAuthRepositoryTest {
    @Test
    fun mapsPollStatusCodes() {
        assertEquals(TraktPollResult.Pending, TraktDeviceAuthRepository.mapPollStatus(400, null))
        assertEquals(TraktPollResult.InvalidDeviceCode, TraktDeviceAuthRepository.mapPollStatus(404, null))
        assertEquals(TraktPollResult.AlreadyUsed, TraktDeviceAuthRepository.mapPollStatus(409, null))
        assertEquals(TraktPollResult.Expired, TraktDeviceAuthRepository.mapPollStatus(410, null))
        assertEquals(TraktPollResult.Denied, TraktDeviceAuthRepository.mapPollStatus(418, null))
        assertEquals(TraktPollResult.SlowDown, TraktDeviceAuthRepository.mapPollStatus(429, null))
        assertEquals(TraktPollResult.Failed("HTTP 500"), TraktDeviceAuthRepository.mapPollStatus(500, null))
    }

    @Test
    fun tokenExpiryUsesSixtySecondLeeway() {
        assertEquals(true, TraktDeviceAuthRepository.isTokenUsable(nowSeconds = 1000, expiresAtSeconds = 1200))
        assertEquals(false, TraktDeviceAuthRepository.isTokenUsable(nowSeconds = 1000, expiresAtSeconds = 1050))
    }
}
